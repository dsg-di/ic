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
package gr.uoa.di.dsg.ic.multivalued;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.consensus.bracha.BBConsensus;
import gr.uoa.di.dsg.consensus.multivalued.MVConsensus;
import gr.uoa.di.dsg.consensus.multivalued.messages.MVInitMessage;
import gr.uoa.di.dsg.consensus.multivalued.messages.MVVectorMessage;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.ic.ICFirstPhase;
import gr.uoa.di.dsg.ic.ICResult;
import gr.uoa.di.dsg.ic.bracha.ICChunkMessage;
import gr.uoa.di.dsg.utils.BroadcastID;
import gr.uoa.di.dsg.utils.GlobalVariables;
import gr.uoa.di.dsg.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MVInteractiveConsistency implements Application {

	/** The unique ID of this Interactive Consistency algorithm. */
	private final String ICID;
	
	/** The node's unique ID. */
	private final int nodeID;
	
	/** The total number of nodes in the system. */
	private final int totalNodes;
	
	/** The maximum number of faults. */
	private int faults;
	
	/** The initial values of all nodes in the system. */
	private final Map<Integer, String> initialValues = new HashMap<>();
	
	/** The processor associated with the completion time of the first phase. */
	protected ICFirstPhase firstPhaseProcessor;

	/** The active MultiValued Consensus instances. */
	private final Map<Integer, MVConsensus> activeMultiValuedConsensus = new HashMap<>();
	
	/** The active binary Consensus instances. */
	private final Map<Integer, BBConsensus> activeBinaryConsensus = new HashMap<>();
	
	/** The active MultiValued Consensus instances. */
	private final Map<Integer, String> completedMultiValuedConsensus = new HashMap<>();
	
	/** The completed binary Consensus instances. */
	private final Map<Integer, String> completedBinaryConsesus = new HashMap<>();
	
	/** The Multicast module of this Interactive Consistency algorithm. */
	private final IBroadcast multicastModule;
	
	/** The reliable Broadcast module of this Interactive Consistency algorithm. */
	private final IBroadcast broadcastModule;
	
	/** The result processor of this Interactive Consistency algorithm. */
	private final ICResult resultProcessor;
	
	/** The timeout information maintained for this Interactive Consistency algorithm. */
	private Object timeout = null;
	private boolean timeoutFired = false;
	private boolean completed = false;
	
	private final String nodeGroup;

	public MVInteractiveConsistency(String ICID, int nodeID, int totalNodes, IBroadcast multicastModule, IBroadcast broadcastModule, ICResult resultProcessor, ICFirstPhase firstPhaseProcessor, String nodeGroup) {
		this.ICID = ICID;
		this.nodeID = nodeID;
		this.totalNodes = totalNodes;
		this.multicastModule = multicastModule;
		this.broadcastModule = broadcastModule;
		this.resultProcessor = resultProcessor;
		this.firstPhaseProcessor = firstPhaseProcessor;
		this.nodeGroup = nodeGroup;
		
		/* Specify the maximum number of faults. */
		this.faults = (int) Math.floor((totalNodes - 1) / 3.0);
	}
	
	@Override
	public void start(int nodeId, String value) {
		timeout = this.multicastModule.getCommunicator().setTimeout(GlobalVariables.TIMEOUT, ()-> onTimeout());
		
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[MVInteractiveConsistency, Node: " + nodeId + "]: Starting one MVIC for <" + value + ">");
		
		multicastModule.broadcast(ICID, -1, nodeId, BroadcastID.MVINITIAL_BROADCAST_ID.getValue(), value);
	}
	
	@Override
	public void start() {
		throw new RuntimeException("This operation is not supported by a MVInteractiveConsistency application!");
	}
	
	@Override
	public void processDatumChunk(ICChunkMessage icChunkMessage) {
		throw new RuntimeException("This operation is not supported by a MVInteractiveConsistency application!");
	}
	
	@Override
	public boolean verifyDatum(String value, int sourceNodeID) {
		throw new RuntimeException("This operation is not supported by a MVInteractiveConsistency application!");
	}
	
	public void onTimeout() {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[MVInteractiveConsistency, Node: "+ nodeID + "]: " + "A timeout was fired with initial values " + Arrays.asList(initialValues).toString());
		
		if(initialValues.size() < (totalNodes - faults))
			System.out.println("[MVInteractiveConsistency, Node: "+ nodeID + "]: " + "WARNING: A timeout was fired, but initial values are missing: " + Arrays.asList(initialValues).toString());
		
		timeoutFired = true;
		if(!completed)
			beginMultiValuedConsensus();
	}
	
	public void beginMultiValuedConsensus() {
		/* Mark the completion of the first phase. */
		this.firstPhaseProcessor.processFirstPhaseCompletion(this.ICID);

		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[MVInteractiveConsistency, Node: "+ nodeID + "]: " + "Beginning MVConsensus with initial values: " + Arrays.asList(initialValues));
		
		/* The node must participate in the MultiValued instances of the IC protocol. */
		List<Integer> nodeIDs = this.broadcastModule.getCommunicator().getNodeIDsOfGroup(nodeGroup);
		for (Integer nodeId: nodeIDs) {
			MVConsensus mvConsensus = activeMultiValuedConsensus.get(nodeId);
			if(mvConsensus == null) {
				mvConsensus = new MVConsensus(this, this.nodeID, this.totalNodes);
				activeMultiValuedConsensus.put(nodeId, mvConsensus);
			}
			
			String value = initialValues.get(nodeId);
			if(value == null)
				value = "null";
			
			mvConsensus.start(ICID, nodeId, value);
		}
	}

	@Override
	public void process(Message msg) {
		if(msg.getClass().equals(BroadcastAccept.class)) {
			BroadcastAccept acceptMsg = (BroadcastAccept) msg;
			
			if(acceptMsg.getBroadcastID() == BroadcastID.MVINITIAL_BROADCAST_ID.getValue()) {
				if(timeoutFired)
					return;
				
				if(GlobalVariables.HIGH_VERBOSE)
					System.out.println("[MVInteractiveConsistency, Node: " + nodeID + "]: An MVInitial value was received: <" + acceptMsg.getApplicationID() + ", " + acceptMsg.getConsensusID()
							+ ", " + acceptMsg.getNodeID() + ", " + acceptMsg.getValue() + ">");
				
				/* The received message is related to the initial multicast phase. */
				initialValues.put(acceptMsg.getNodeID(), acceptMsg.getValue());
				
				if(initialValues.size() == totalNodes) {
					/* Cancel the timeout. */
					completed = true;
					this.multicastModule.getCommunicator().cancelTimeout(timeout);
					
					/* Start the MultiValued Consensus instances. */
					beginMultiValuedConsensus();
				}
			}
			else if(acceptMsg.getBroadcastID() == BroadcastID.MVINIT_BROADCAST_ID.getValue()) {
				/* The received message is related to the 1st phase of some MultiValued Consensus instance. */
				
				if(GlobalVariables.HIGH_VERBOSE)
					System.out.println("[MVInteractiveConsistency, Node: " + nodeID + "]: An MVInit value was received: <" + acceptMsg.getApplicationID() + ", " + acceptMsg.getConsensusID()
							+ ", " + acceptMsg.getNodeID() + ", " + acceptMsg.getValue() + ">");
				
				if (completedMultiValuedConsensus.containsKey(acceptMsg.getConsensusID()))
					return;
				
				MVConsensus mvConsensus = activeMultiValuedConsensus.get(acceptMsg.getConsensusID());
				if (mvConsensus == null) {
					mvConsensus = new MVConsensus(this, this.nodeID, this.totalNodes);
					activeMultiValuedConsensus.put(acceptMsg.getConsensusID(), mvConsensus);
				}
				
				mvConsensus.process(new MVInitMessage(acceptMsg));
			}
			else if(acceptMsg.getBroadcastID() == BroadcastID.MVVECTOR_BROADCAST_ID.getValue()) {
				/* The received message is related to the 2nd phase of some MultiValued Consensus instance. */
				
				if(GlobalVariables.HIGH_VERBOSE)
					System.out.println("[MVInteractiveConsistency, Node: " + nodeID + "]: An MVVector value was received: <" + acceptMsg.getApplicationID() + ", " + acceptMsg.getConsensusID()
							+ ", " + acceptMsg.getNodeID() + ", " + acceptMsg.getValue() + ">");
				
				if (completedMultiValuedConsensus.containsKey(acceptMsg.getConsensusID()))
					return;
				
				MVConsensus mvConsensus = activeMultiValuedConsensus.get(acceptMsg.getConsensusID());
				if (mvConsensus == null) {
					mvConsensus = new MVConsensus(this, this.nodeID, this.totalNodes);
					activeMultiValuedConsensus.put(acceptMsg.getConsensusID(), mvConsensus);
				}
				
				MVVectorMessage vectMsg = new MVVectorMessage(acceptMsg);
				String[] splitValues = acceptMsg.getValue().split(";");
				
				/* Verify that the value of the message has the proper form. */
				//assert splitValues.length == 2;
				
				vectMsg.setVectorOfValues(StringUtils.stringToArray(splitValues[1]));
				vectMsg.setValue(splitValues[0]);
				
				mvConsensus.process(vectMsg);
			}
			else if(acceptMsg.getBroadcastID() == BroadcastID.MVPROPOSE_BROADCAST_ID.getValue()) {
				BBConsensus consensus = activeBinaryConsensus.get(acceptMsg.getConsensusID());
				if(consensus == null) {
					consensus = new BBConsensus(this, ICID, this.nodeID, acceptMsg.getConsensusID(), totalNodes);
					activeBinaryConsensus.put(acceptMsg.getConsensusID(), consensus);
				}
				consensus.start(acceptMsg.getValue());
			}
			else if(acceptMsg.getBroadcastID() == BroadcastID.MVACCEPT_BROADCAST_ID.getValue()) {
				
				if(GlobalVariables.HIGH_VERBOSE)
					System.out.println("[MVInteractiveConsistency, Node: " + nodeID + "]: An MVConsensus instance has terminated: <" + acceptMsg.getApplicationID() + ", " + acceptMsg.getConsensusID() + ">");
				
				completedMultiValuedConsensus.put(acceptMsg.getConsensusID(), acceptMsg.getValue());
				
				if(completedMultiValuedConsensus.size() == this.totalNodes)
					this.resultProcessor.processResult(this.ICID, new ArrayList<String>(completedMultiValuedConsensus.values()));
			}
			else if(acceptMsg.getBroadcastID() >= 0) {
				/* The received message is related to some binary Consensus instance. */
				
				if (completedBinaryConsesus.containsKey(acceptMsg.getConsensusID()))
					return;

				BBConsensus consensus = activeBinaryConsensus.get(acceptMsg.getConsensusID());
				if (consensus == null) {
					consensus = new BBConsensus(this, ICID, this.nodeID, acceptMsg.getConsensusID(), totalNodes);
					activeBinaryConsensus.put(acceptMsg.getConsensusID(), consensus);
				}
				consensus.process(acceptMsg);
			}
			else {
				System.out.println("The MVInteractiveConsistency module received an unknown broadcast ID: " + msg.toString());
				System.exit(-1);
			}
		}
		else {
			System.out.println("The MVInteractiveConsistency module received an unknown message type: " + msg.getClass() + ", " + msg.toString());
			System.exit(-1);
		}
	}

	@Override
	public void processConsensusResult(int cid, String value) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Node " + nodeID + "]: Processing result " + value + " for Consensus with ID: " + cid);
		
		completedBinaryConsesus.put(cid, value);
		MVConsensus mvConsensus = activeMultiValuedConsensus.get(cid);
		if (mvConsensus == null) {
			mvConsensus = new MVConsensus(this, this.nodeID, this.totalNodes);
			activeMultiValuedConsensus.put(cid, mvConsensus);
		}
		
		mvConsensus.processConsensusResult(ICID, cid, value);
	}

	@Override
	public IBroadcast getBroadcast() {
		return broadcastModule;
	}
}
