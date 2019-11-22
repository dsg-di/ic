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
package gr.uoa.di.dsg.broadcast.multicast;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.consensus.multivalued.messages.MVInitialMessage;
import gr.uoa.di.dsg.consensus.multivalued.messages.MVMessageType;
import gr.uoa.di.dsg.ic.ApplicationGetter;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author stathis
 *
 */
public class Multicast implements IBroadcast {

	/** The logger instance of this class. */
	//private final static Logger logger = LoggerFactory.getLogger(Multicast.class);

	/**
	 * The Communicator instance of this class.
	 */
	private transient AbstractCommunicator communicator = null;
	
	/**
	 * The Application instance that uses this broadcast.
	 */
	private transient ApplicationGetter applicationGetter = null;
	
	/** 
	 * The set of incoming MVInitialMessage.
	 */
	private Map<String, Set<Integer>> storedMessages = null;
	
	/** The communication group that this broadcast module belongs. */
	private String nodeGroup = null;
	
	@Override
	public void initialize(AbstractCommunicator comm, ApplicationGetter appGetter, String nodeGroup, int numNodes) {
		comm.registerMessage(MVMessageType.MVINITIAL.getValue(), (byte[] data) -> MVInitialMessage.deserialize(data), (Message msg, Node source) -> onMVInitial((MVInitialMessage) msg, source));
		
		this.communicator = comm;
		this.applicationGetter = appGetter;
		this.nodeGroup = nodeGroup;
		this.storedMessages = new HashMap<>();
	}

	@Override
	public void broadcast(String icid, int cid, int pid, int bid, String value) {
		MVInitialMessage msg = new MVInitialMessage(icid, cid, pid, bid, value);
		
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Multicast, Node: " + pid + "]: Starting a broadcast for " + msg.toString());
		
		this.communicator.sendGroup(nodeGroup, msg);
	}
	
	@Override
	public AbstractCommunicator getCommunicator() {
		return this.communicator;
	}
	
	/**
	 * The handler for the {@link MVInitialMessage}. The recipient node process the received message,
	 * extracts its proposed value and stores the message.
	 * 
	 * @param message the message containing the private value of the specified sender node.
	 * @param source the sender node that multicasts the specified message.
	 */
	public void onMVInitial(MVInitialMessage message, Node source) {
		if(GlobalVariables.LOW_VERBOSE)
			System.out.println("[Multicast, Node: " + communicator.getCurrentNode().getNodeId() + "]: A MVInitialMessage was received: " + message.toString() + " from Node " + source.getNodeId());
		
		/* Verify that the message meets its specification. */
		//assert message.getBroadcastID() == BroadcastID.MVINITIAL_BROADCAST_ID.getValue();
		
		/* Create a unique ID, based on the message's parameters. */
		String UUID = message.getApplicationID() + ":" + message.getNodeID() + ":" + MVMessageType.MVINITIAL;
		
		/* Get the current set of stored messages. */
		Set<Integer> storedNodeIDs = storedMessages.get(UUID);
		if(storedNodeIDs == null) {
			storedNodeIDs = new HashSet<>();
			storedMessages.put(UUID, storedNodeIDs);
		}
		
		/* Ignore duplicate or faulty messages. */
		if(storedNodeIDs.contains(source.getNodeId())) {
			System.err.println("The node with ID: " + communicator.getCurrentNode().getNodeId() + " received another MVInitialMessage from node with ID: " + source.getNodeId());
			return;
		}
		else
			storedNodeIDs.add(source.getNodeId());
		
		/* Make an up-call with the delivered message. */
		BroadcastAccept deliverMessage = new BroadcastAccept(message.getApplicationID(), message.getConsensusID(), message.getNodeID() , message.getBroadcastID(), message.getValue());
		applicationGetter.getApp(message.getApplicationID()).process(deliverMessage);
	}
}
