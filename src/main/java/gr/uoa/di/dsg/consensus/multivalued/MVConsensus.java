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
package gr.uoa.di.dsg.consensus.multivalued;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.broadcast.BroadcastMessage;
import gr.uoa.di.dsg.consensus.multivalued.messages.MVInitMessage;
import gr.uoa.di.dsg.consensus.multivalued.messages.MVMessageType;
import gr.uoa.di.dsg.consensus.multivalued.messages.MVVectorMessage;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.utils.BroadcastID;
import gr.uoa.di.dsg.utils.GlobalVariables;
import gr.uoa.di.dsg.utils.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MVConsensus {
	
	/** The number of totalNodes in the system. */
	private final int totalNodes;

	/** The Logger instance for this class. */
	//private final static Logger logger = LoggerFactory.getLogger(MVConsensus.class);

	/** The maximum number of faults. */
	private final int faults;
	
	/** The Application instance of this module. */
	private final Application application;

	/** The nodeID of this instance. */
	private final int nodeID;
	
	/**
	 * The node's vector of MVInit values for every running instance of
	 * interactive consistency and consensus.
	 */
	private Map<String, String[]> initValues = null;

	/**
	 * The node's vector of MVVector "weighted" values for every running
	 * instance of interactive consistency and consensus.
	 */
	private Map<String, String[]> weightedValues = null;
	
	/** A set containing all binary Consensus instances the node has participated. */
	private Set<String> votedForConsensus = null;
	
	/** The result of its binary Consensus instance. */
	private Map<String, String> consensusResults = null;

	/** The node's set of incoming messages. */
	private Map<String, Set<BroadcastMessage>> incomingMessages = null;

	/** The node's set of non-validated MVVector messages. */
	private Map<String, Set<BroadcastMessage>> nonValidatedMessages = null;

	/** The set of all committed vectors. */
	private Map<String, String> committedValues = null;
	
	public MVConsensus(Application application, int nodeID, int totalNodes) {
		this.application = application;
		this.totalNodes = totalNodes;
		this.nodeID = nodeID;
		
		/* Initialize all data structures. */
		this.initValues = new HashMap<String, String[]>();
		this.weightedValues = new HashMap<String, String[]>();
		this.incomingMessages = new HashMap<String, Set<BroadcastMessage>>();
		this.nonValidatedMessages = new HashMap<String, Set<BroadcastMessage>>();
		this.committedValues = new HashMap<String, String>();
		this.consensusResults = new HashMap<String, String>();
		this.votedForConsensus = new HashSet<String>();
		
		/* Specify the maximum number of faults. */
		this.faults = (int) Math.floor((totalNodes - 1) / 3.0);
	}
	
	public void start(String icid, int cid, String value) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[MultiValuedConsensus, Node: " + nodeID + "]: Starting one MC for <" + icid + ", " + cid + ", " + value + ">");
			
		this.application.getBroadcast().broadcast(icid, cid, nodeID, BroadcastID.MVINIT_BROADCAST_ID.getValue(), value);
	}
	
	public void clear(String ICID) {
		/* Remove all messages/values regarding the MVInitMessages. */
		String UUID = ICID + ":" + MVMessageType.MVINIT;
		this.incomingMessages.remove(UUID);
		this.initValues.remove(UUID);
		
		/* Remove all messages/values regarding the MVVectorMessages. */
		UUID = ICID + ":" + MVMessageType.MVVECTOR;
		this.incomingMessages.remove(UUID);
		this.initValues.remove(UUID);
		this.weightedValues.remove(UUID);
		this.nonValidatedMessages.remove(UUID);
	}

	public void process(MVInitMessage initMsg) {
		if(GlobalVariables.LOW_VERBOSE)
			System.out.println("[MultiValuedConsensus, Node: " + nodeID + "]: A MVInitMessage was received: " + initMsg.toString() + " from Node " + initMsg.getNodeID());
		
		String ICID = initMsg.getApplicationID();
		Integer CID = initMsg.getConsensusID();
		Integer PID = initMsg.getNodeID();
		
		/* Verify that the message satisfies its specification. */
		//assert initMsg.getBroadcastID() == BroadcastID.MVINIT_BROADCAST_ID.getValue();
		
		/* Ignore the message if the Consensus instance for it has already been completed. */
		String commitUUID = ICID + ":" + CID;
		if(committedValues.containsKey(commitUUID))
			return;

		/* Create an identifier, based on the received message. */
		String UUID = ICID + ":" + MVMessageType.MVINIT;

		/* Get all received messages for that identifier. */
		Set<BroadcastMessage> currentMsgs = incomingMessages.get(UUID);
		if (currentMsgs == null)
			currentMsgs = new HashSet<BroadcastMessage>();

		/* Ignore duplicates. */
		for(BroadcastMessage msg: currentMsgs) {
			if(initMsg.isEqual(msg)) {
				//logger.warn("Duplicate message detected: " + initMsg.toString());
				return;
			}
		}

		/* Store the received message. */
		currentMsgs.add(initMsg);
		incomingMessages.put(UUID, currentMsgs);

		/* Update the local vector of received values. */
		String[] values = initValues.get(UUID);
		if (values == null)
			values = new String[totalNodes];

		values[PID] = initMsg.getValue();
		initValues.put(UUID, values);

		/* Check if the new message can validate any MVVector message. */
		Set<BroadcastMessage> pendingMessages = nonValidatedMessages.get(UUID);
		if (pendingMessages != null && pendingMessages.size() > 0)
			validateMessages(ICID, CID);

		/*
		 * Check if the proper number of MVInit messages has been received, in
		 * order to proceed to the second phase.
		 */
		if (currentMsgs.size() == (totalNodes - faults))
			proceedToNextPhase(ICID, CID);
	}

	private void validateMessages(String ICID, int CID) {
		/* Create the appropriate identifiers. */
		String phase1UUID = ICID + ":" + MVMessageType.MVINIT;
		String phase2UUID = ICID + ":" + MVMessageType.MVVECTOR;

		/* Find all non-validated messages. */
		Set<BroadcastMessage> pendingMessages = nonValidatedMessages.get(phase2UUID);
		Set<BroadcastMessage> currentMsgs = incomingMessages.get(phase1UUID);

		/* Check if any message can be validated. */
		for (Iterator<BroadcastMessage> ite = pendingMessages.iterator(); ite.hasNext();) {
			BroadcastMessage msg = ite.next();
			if (validateMessage(msg)) {
				/* Remove the message from the non-validated set and store it. */
				ite.remove();
				currentMsgs.add(msg);
			}
		}

		/* Update the current set of non-validated Messages. */
		nonValidatedMessages.put(phase2UUID, pendingMessages);

		/*
		 * Check if the proper number of MVVector messages has been received, in
		 * order to proceed to the final phase.
		 */
		if (currentMsgs.size() == (totalNodes - faults))
			proceedToFinalPhase(ICID, CID);
	}

	private boolean validateMessage(BroadcastMessage message) {
		/* Verify that only MVVector messages will be validated. */
		//assert message.getClass().equals(MVVectorMessage.class);
		
		MVVectorMessage vectMessage = (MVVectorMessage) message;

		String ICID = message.getApplicationID();
		Integer CID = message.getConsensusID();

		/* Verify that the message satisfies its specification. */
		//assert message.getBroadcastID() == BroadcastID.MVVECTOR_BROADCAST_ID.getValue();

		/* Create an identifier, based on the received message. */
		String UUID = ICID + ":" + MVMessageType.MVINIT;
		
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[MultiValuedConsensus, Node: " + nodeID + "]: In validateMessage, incomingMessages: " + Arrays.asList(incomingMessages));

		Set<BroadcastMessage> currentMsgs = incomingMessages.get(UUID);
		if (currentMsgs == null)
			return false;

		/* Verify that the appropriate MVInit messages have been received. */
		String[] vectorOfValues = vectMessage.getVectorOfValues();
		for (int i = 0; i < vectorOfValues.length; ++i) {
			if (vectorOfValues[i] != null && !"null".equalsIgnoreCase(vectorOfValues[i])) {
				MVInitMessage initMessage = new MVInitMessage(ICID, CID, i, BroadcastID.MVINIT_BROADCAST_ID.getValue(), vectorOfValues[i]);

				/* Check if the appropriate MVInitMessage has been received. */
				boolean found = false;
				for(BroadcastMessage msg: currentMsgs) {
					if(initMessage.isEqual(msg)) {
						found = true;
						break;
					}
				}
				
				if(!found)
					return false;
			}
		}

		/* Verify that the "weighted" value is the appropriate. */
		String vectMsgWeightedValue = vectMessage.getValue();
		if (vectMsgWeightedValue != null) {
			String correctWeightedValue = calculateMajorityValue(vectMessage.getVectorOfValues());
			if (vectMsgWeightedValue.equals(correctWeightedValue) == false)
				return false;
		}

		return true;
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
		 * If the proper number of processes has the same MVInit value then,
		 * select that value for the second round.
		 */
		if (count >= (totalNodes - (2 * faults)))
			return candidate;
		else
			return null;
	}

	private void proceedToNextPhase(String ICID, Integer CID) {
		/* Create an identifier, based on the received message. */
		String UUID = ICID + ":" + MVMessageType.MVINIT;
		
		/* Calculate the node's "weighted" value. */
		String[] initialValues = initValues.get(UUID);
		String majorityValue = calculateMajorityValue(initialValues);

		/* Broadcast a MVVectorMessage containing the "weighted" value. */
		String value = majorityValue + ";" + StringUtils.arrayToString(initialValues);
		this.application.getBroadcast().broadcast(ICID, CID, nodeID, BroadcastID.MVVECTOR_BROADCAST_ID.getValue(), value);
	}

	public void process(MVVectorMessage vectorMsg) {
		if(GlobalVariables.LOW_VERBOSE)
			System.out.println("[MultiValuedConsensus, Node: " + nodeID + "]: A MVVectorMessage was received: " + vectorMsg.toString() + " from Node " + vectorMsg.getNodeID());
		
		/* Verify that the message satisfies its specification. */
		//assert vectorMsg.getBroadcastID() == BroadcastID.MVVECTOR_BROADCAST_ID.getValue();

		String ICID = vectorMsg.getApplicationID();
		Integer CID = vectorMsg.getConsensusID();
		Integer PID = vectorMsg.getNodeID();
		
		/* Ignore the message if the Consensus instance for it has already been completed. */
		String commitUUID = ICID + ":" + CID;
		if(committedValues.containsKey(commitUUID))
			return;

		/* Create an identifier, based on the received message. */
		String UUID = ICID + ":" + MVMessageType.MVVECTOR;

		/* Check if the message can be validated. */
		if (!validateMessage(vectorMsg)) {
			/* Add to non-validated messages. */
			Set<BroadcastMessage> pendingMessages = nonValidatedMessages.get(UUID);
			if (pendingMessages == null)
				pendingMessages = new HashSet<BroadcastMessage>();

			pendingMessages.add(vectorMsg);
			nonValidatedMessages.put(UUID, pendingMessages);

			//logger.warn("Couldn't validate message: " + vectorMsg.toString());
			if(GlobalVariables.HIGH_VERBOSE)
				System.out.println("[MultiValuedConsensus, Node " + nodeID + "]: Couldn't validate message: " + vectorMsg.toString()
						+ ", with init values: " + Arrays.asList(initValues.get(ICID + ":" + MVMessageType.MVINIT)));

			return;
		}
		
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[MultiValuedConsensus, Node " + nodeID + "]: Validated message: " + vectorMsg.toString());

		Set<BroadcastMessage> currentMsgs = incomingMessages.get(UUID);
		if (currentMsgs == null)
			currentMsgs = new HashSet<BroadcastMessage>();

		/* Ignore duplicates. */
		for(BroadcastMessage msg: currentMsgs) {
			if(vectorMsg.isEqual(msg)) {
				//logger.warn("Duplicate message detected: " + vectorMsg.toString());
				return;
			}
		}

		/* Store the received message. */
		currentMsgs.add(vectorMsg);
		incomingMessages.put(UUID, currentMsgs);

		/* Update the local vector of received "weighted" values. */
		String[] values = weightedValues.get(UUID);
		if (values == null)
			values = new String[totalNodes];

		values[PID] = vectorMsg.getValue();
		weightedValues.put(UUID, values);
		
		/*
		 * Check if the proper number of MVVector messages has been received, in
		 * order to proceed to the final phase.
		 */
		if (currentMsgs.size() == (totalNodes - faults))
			proceedToFinalPhase(vectorMsg.getApplicationID(), vectorMsg.getConsensusID());
	}

	private void proceedToFinalPhase(String ICID, int CID) {
		String UUID = ICID + ":" + MVMessageType.MVVECTOR;
		String consensusUUID = ICID + ":" + CID;
		
		if(consensusResults.containsKey(consensusUUID))
			terminate(ICID, CID);
		else if(votedForConsensus.contains(consensusUUID))
			return;
		else {
			/* Calculate the node's consensus value. */
			String value = calculateMajorityValue(weightedValues.get(UUID));
			String consensusValue = (value == null) ? "0" : "1";
			
			/* Make an up-call with the proposed value. */
			BroadcastMessage proposeMessage = new BroadcastAccept(ICID, CID, nodeID, BroadcastID.MVPROPOSE_BROADCAST_ID.getValue(), consensusValue);
			votedForConsensus.add(consensusUUID);
			application.process(proposeMessage);
		}
	}
	
	public void processConsensusResult(String ICID, int CID, String value) {
		/* Store the results of consensus. */
		String consensusUUID = ICID + ":" + CID;
		consensusResults.put(consensusUUID, value);
		
		if(value.equalsIgnoreCase("0")) {
			/* Make an up-call with the delivered message. */
			BroadcastMessage acceptMessage = new BroadcastAccept(ICID, CID, nodeID, BroadcastID.MVACCEPT_BROADCAST_ID.getValue(), null);
			committedValues.put(consensusUUID, null);
			application.process(acceptMessage);
		}
		else {
			/* Check if the proper number of MVVectorMessages has been validated. */
			terminate(ICID, CID);
		}
	}
	
	private void terminate(String ICID, int CID) {
		String commitUUID = ICID + ":" + CID;
		
		if(committedValues.containsKey(commitUUID))
			return;
		
		String UUID = ICID + ":" + MVMessageType.MVVECTOR;
		String[] wValues = weightedValues.get(UUID);
		String res = this.calculateMajorityValue(wValues);
		
		if(res != null) {
			/* Make an up-call with the delivered message. */
			BroadcastAccept acceptMessage = new BroadcastAccept(ICID, CID, nodeID, BroadcastID.MVACCEPT_BROADCAST_ID.getValue(), res);
			committedValues.put(commitUUID, res);
			
			/* Clear as much memory as possible. */
			this.clear(ICID);
			
			application.process(acceptMessage);
		}
	}
}
