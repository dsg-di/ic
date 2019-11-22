/*******************************************************************************
 * Copyright (C) 2019 DSG at University of Athens
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package gr.uoa.di.dsg.ic.lamport;

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.ic.ApplicationGetter;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SynchronousICBroadcast implements IBroadcast {

	/** The number of totalNodes in the system. */
	private int totalNodes;
	
	/** The current round of the protocol per application. */
	private Map<String, Integer> currentRoundPerApp;

	/** The maximum number of faults. */
	private int faults;
	
	/** The Communicator instance of this class. */
	private transient AbstractCommunicator communicator = null;
	
	/** The Application instance that uses this broadcast. */
	private transient ApplicationGetter applicationGetter = null;
	
	/** The vector of values. */
	private Map<String, String[]> vectorOfValues = null;
	
	/** The start time of each application. */
	private Map<String, Long> startTimePerApp = null;
	
	/** The set of all received ICInit messages. */
	private Map<String, Set<Message>> receivedMessages = null;
	
	/** The timeout information maintained for this broadcast mechanism. */
	private Map<String, Object> timeoutPerApp = null;

	/** The timeout value for each individual round. */
	private final int TIMEOUT_VALUE = GlobalVariables.TIMEOUT;
	
	/** The communication group that this broadcast module belongs. */
	private String nodeGroup = null;
	
	@Override
	public void initialize(AbstractCommunicator comm, ApplicationGetter appGetter, String nodeGroup, int numNodes) {
		comm.registerMessage(SynchronousICMessageType.ICINIT.getValue(), (byte[] data) -> ICInitMessage.deserialize(data), (Message msg, Node source) -> onICInit((ICInitMessage) msg, source));
		comm.registerMessage(SynchronousICMessageType.ICVECTOR.getValue(), (byte[] data) -> ICVectorMessage.deserialize(data), (Message msg, Node source) -> onICVector((ICVectorMessage) msg, source));
		
		this.communicator = comm;
		this.applicationGetter = appGetter;
		this.nodeGroup = nodeGroup;
		this.totalNodes = numNodes;
		
		/* Initialize all data structures. */
		this.vectorOfValues = new HashMap<>();
		this.receivedMessages = new HashMap<>();
		this.currentRoundPerApp = new HashMap<>();
		this.timeoutPerApp = new HashMap<>();
		this.startTimePerApp = new HashMap<>();
		
		/* Specify the maximum number of faults. */
		this.faults = (int) Math.floor((totalNodes - 1) / 3.0);
	}
	
	@Override
	public void broadcast(String icid, int cid, int pid, int bid, String value) {
		this.currentRoundPerApp.put(icid, 1);
		
		Object timeout = communicator.setTimeout(TIMEOUT_VALUE, ()-> onTimeout(icid) );
		this.timeoutPerApp.put(icid, timeout);
		
		ICInitMessage icInitMessage = new ICInitMessage(icid, cid, pid, value);
		
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Node " + communicator.getCurrentNode().getNodeId() + "]: Starting a SynchronousICBroadcast with " + icInitMessage.toString() + " in appID " + icid);
		
		/* Mark the start time of each application so that future timeout values can be calculated properly. */
		this.startTimePerApp.put(icid, System.currentTimeMillis());
		
		this.communicator.sendGroup(nodeGroup, icInitMessage);
	}
	
	public void onTimeout(String ICID) {
		int currentRound = currentRoundPerApp.get(ICID);
		
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Node: " + communicator.getCurrentNode().getNodeId() + "]: ******* Timeout fired ******** during round: " + currentRound
						+ " for ICID: " + ICID + " with values: " + Arrays.toString(vectorOfValues.get(String.valueOf(ICID))));
		
		if(currentRound == 1) {
			/* First round. */
			++currentRound;
			
			currentRoundPerApp.put(ICID, currentRound);
			
			Object timeout = communicator.setTimeout(TIMEOUT_VALUE, ()-> onTimeout(ICID) );
			timeoutPerApp.put(ICID, timeout);
			
			String[] values = vectorOfValues.get(ICID);
			this.communicator.sendGroup(nodeGroup, new ICVectorMessage(ICID, currentRound, this.communicator.getCurrentNode().getNodeId(), values));
		}
		else if(currentRound <= (faults + 1))
			this.proceedToNextRound(ICID, currentRound);
	}
	
	public void onICInit(ICInitMessage message, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Node " + communicator.getCurrentNode().getNodeId() + "]: An ICInitMessage was received: " + message.toString() + " from Node " + source.getNodeId());
		
		String ICID = message.getApplicationID();
		int roundID = message.getRound();
		int PID = message.getProcessID();
		
		assert roundID <= (faults + 1);
		
		/* Create an identifier, based on the received message. */
		String UUID = ICID + ":" + roundID + ":" + SynchronousICMessageType.ICINIT;

		/* Get all received messages for that identifier. */
		Set<Message> msgs = receivedMessages.get(UUID);
		if (msgs == null)
			msgs = new HashSet<>();

		/* Ignore duplicates. */
		for(Message msg: msgs) {
			if(message.isEqual(msg)) {
				System.err.println("[Node " + communicator.getCurrentNode().getNodeId() + "]: Duplicate message detected: " + message.toString() + " from Node " + source.getNodeId() + " in appID " + ICID);
				return;
			}
			else if(source.getNodeId() == ((ICInitMessage) msg).getProcessID()) {
				System.err.println("The node with ID: " + communicator.getCurrentNode().getNodeId() + " received another ICInitMessage from node with ID: " + source.getNodeId());
			}
		}

		/* Store the received message. */
		msgs.add(message);
		receivedMessages.put(UUID, msgs);

		/* Update the local vector of received values. */
		String[] values = vectorOfValues.get(ICID);
		if (values == null)
			values = new String[totalNodes];

		values[PID] = message.getValue();
		vectorOfValues.put(ICID, values);

		/*
		 * Check if the proper number of ICInit messages has been received, in
		 * order to proceed to the second phase.
		 */
		if (msgs.size() == totalNodes) {
			Object timeout = timeoutPerApp.remove(ICID);
			if(timeout == null) {
				System.err.println("[SynchronousICBroadcast, Node " + communicator.getCurrentNode().getNodeId() + "]: The timeout object for ICID " + ICID + " does not exist!");
				System.exit(-1);
			}
			communicator.cancelTimeout(timeout);
			
			/* Proceed to the next round. */
			currentRoundPerApp.put(ICID, ++roundID);
			
			/* Calculate the new timeout value based on the specified timeout value per round. */
			long new_timeout_value = (roundID * TIMEOUT_VALUE * 1000) - (System.currentTimeMillis() - this.startTimePerApp.get(ICID));
			timeout = communicator.setTimeout(new_timeout_value, ()-> onTimeout(ICID) );
			timeoutPerApp.put(ICID, timeout);
			
			this.communicator.sendGroup(nodeGroup, new ICVectorMessage(ICID, roundID, this.communicator.getCurrentNode().getNodeId(), values));
		}
	}

	public void onICVector(ICVectorMessage message, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Node " + communicator.getCurrentNode().getNodeId() + "]: An ICVectorMessage was received: " + message.toString() + " from Node " + source.getNodeId());
		
		String ICID = message.getApplicationID();
		Integer roundID = message.getRound();
		
		assert roundID <= (faults + 1);
		
		/* Create an identifier, based on the received message. */
		String UUID = ICID + ":" + roundID + ":" + SynchronousICMessageType.ICVECTOR;
		
		/* Get all received messages for that identifier. */
		Set<Message> msgs = receivedMessages.get(UUID);
		if (msgs == null)
			msgs = new HashSet<>();

		/* Ignore duplicates. */
		for(Message msg: msgs) {
			if(message.isEqual(msg)) {
				System.err.println("[Node " + communicator.getCurrentNode().getNodeId() + "]: Duplicate message detected: " + message.toString() + " from Node " + source.getNodeId() + " in appID " + ICID);
				return;
			}
			else if(message.getProcessID() == ((ICVectorMessage) msg).getProcessID()) {
				System.err.println("The node with ID: " + communicator.getCurrentNode().getNodeId() + " received another ICVectorMessage from node with ID: " + source.getNodeId());
			}
		}

		/* Store the received message. */
		msgs.add(message);
		receivedMessages.put(UUID, msgs);
		
		/* Proceed to the next round, if all necessary messages have been received. */
		if(msgs.size() == totalNodes)
			this.proceedToNextRound(ICID, roundID);
	}
	
	private void proceedToNextRound(String ICID, Integer roundID) {
		/* Create an identifier, based on the received message. */
		String UUID = ICID + ":" + roundID + ":" + SynchronousICMessageType.ICVECTOR;
		
		/* Get all received messages for that identifier. */
		Set<Message> msgs = receivedMessages.get(UUID);
		
		/* Get the current vector of values. */
		String[] values = vectorOfValues.get(ICID);
		
		/* Create a list containing all received messages for this round. */
		List<Message> vectorMsgs = new ArrayList<Message>();
		if(msgs != null)
			vectorMsgs.addAll(msgs);
		
		/* Update the values inside the vector. */
		for(int i = 0; i < vectorMsgs.size(); ++i) {
			/* Create an intermediate vector containing all values in index i. */
			String[] intermediateValues = new String[vectorMsgs.size()];
			for(int j = 0; j < vectorMsgs.size(); ++j)
				intermediateValues[j] = ((ICVectorMessage) vectorMsgs.get(j)).getValue(i);
			
			/* Find the majority value, if exists, in the intermediate vector
			 * and update the appropriate index. */
			values[i] = this.calculateMajorityValue(intermediateValues);
		}
		
		/* Store the updated vector of values. */
		vectorOfValues.put(ICID.toString(), values);
		
		/* Cancel the timeout for this round. */
		Object timeout = timeoutPerApp.remove(ICID);
		if(timeout == null) {
			System.err.println("[SynchronousICBroadcast, Node " + communicator.getCurrentNode().getNodeId() + "]: The timeout object for ICID " + ICID + " does not exist!");
			System.exit(-1);
		}
		communicator.cancelTimeout(timeout);
		
		/* Check if the algorithm has more rounds. */
		if(roundID == (faults + 1)) {
			ICDeliverMessage deliverMessage = new ICDeliverMessage(ICID, vectorOfValues.get(ICID));
			applicationGetter.getApp(ICID).process(deliverMessage);
		}
		else {
			/* Proceed to the next round. */
			currentRoundPerApp.put(ICID, ++roundID);
			
			/* Calculate the new timeout value based on the specified timeout value per round. */
			long new_timeout_value = (roundID * TIMEOUT_VALUE * 1000) - (System.currentTimeMillis() - this.startTimePerApp.get(ICID));
			timeout = communicator.setTimeout(new_timeout_value, ()-> onTimeout(ICID) );
			timeoutPerApp.put(ICID, timeout);
			this.communicator.sendGroup(nodeGroup, new ICVectorMessage(ICID, roundID + 1, this.communicator.getCurrentNode().getNodeId(), vectorOfValues.get(ICID.toString())));
		}
	}
	
	/* http://gregable.com/2013/10/majority-vote-algorithm-find-majority.html */
	private String calculateMajorityValue(String[] vectorWithValues) {
		int count = 0;
		String candidate = null;

		/* Find the majority value inside the vector of values. */
		for (String val : vectorWithValues) {
			if(val == null)
				continue;
			
			if (count == 0)
				candidate = val;

			if (candidate.equals(val))
				++count;
			else
				--count;
		}

		/* Count the references of the majority value. */
		count = 0;
		for (String val : vectorWithValues)
			if (candidate.equals(val))
				++count;

		/*
		 * If the proper number of processes has the same ICInit value then,
		 * select that value for the second round.
		 */
		if(count >= ((2 * faults) + 1))
			return candidate;
		else
			return null;
	}

	@Override
	public AbstractCommunicator getCommunicator() {
		return this.communicator;
	}
}
