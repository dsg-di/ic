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
package gr.uoa.di.dsg.ic.bracha;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcast;
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcastAccept;
import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.consensus.bracha.BBConsensus;
import gr.uoa.di.dsg.crypto.CryptographyModule;
import gr.uoa.di.dsg.crypto.DigitalSignatureCryptographyModule;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.ic.ICFirstPhase;
import gr.uoa.di.dsg.ic.ICResult;
import gr.uoa.di.dsg.ic.recovery.RecoveryRequestMessage;
import gr.uoa.di.dsg.ic.recovery.RecoveryResponseMessage;
import gr.uoa.di.dsg.utils.BroadcastID;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BBInteractiveConsistency implements Application {
	
	protected String id;
	public int nodeID;
	protected int numberOfNodes;
	protected int faults;
	
	/** The signatures accompanying the initial values spread in the value dissemination phase. */
	protected Map<Integer, Map<Integer, byte[]>> nodeValueSignatures = new HashMap<>();
	protected Map<Integer, String> nodeValue = new HashMap<>();
	protected ICFirstPhase firstPhaseProcessor;
	
	protected Map<Integer, BBConsensus> activeConsensus = new HashMap<>();
	protected Map<Integer, String> completedConsensus = new HashMap<>();
	
	protected IBroadcast constBroadcast = null;
	protected final int defaultConsensusID = -1;
	
	protected IBroadcast relBroadcast = null;
	protected ICResult resultProcessor = null;
	protected Object timeout = null;
	protected boolean completed = false;
	protected boolean timeoutFired = false;
	
	protected final String nodeGroup;
	
	public BBInteractiveConsistency(String id, int nodeID, int numNodes, IBroadcast relBroadcast, IBroadcast constBroadcast, ICResult resultProcessor, ICFirstPhase firstPhaseProcessor, String nodeGroup) {
		this.id = id;
		this.nodeID = nodeID;
		this.numberOfNodes = numNodes;
		this.relBroadcast = relBroadcast;
		this.constBroadcast = constBroadcast;
		this.resultProcessor = resultProcessor;
		this.firstPhaseProcessor = firstPhaseProcessor;
		this.nodeGroup = nodeGroup;
		
		/* Specify the maximum number of faults. */
		this.faults = (int) Math.floor((numberOfNodes - 1) / 3.0);
	}
	
	@Override
	public void start(int pid, String value) {
		timeout = this.constBroadcast.getCommunicator().setTimeout(GlobalVariables.TIMEOUT, ()->onTimeout() );
		constBroadcast.broadcast(this.id, defaultConsensusID, pid, BroadcastID.CB_BROADCAST_ID.getValue(), value);
	}
	
	@Override
	public void start() {
		throw new RuntimeException("This operation is not supported by a BBInteractiveConsistency application!");
	}
	
	@Override
	public void processDatumChunk(ICChunkMessage icChunkMessage) {
		throw new RuntimeException("This operation is not supported by a BBInteractiveConsistency application!");
	}

	@Override
	public boolean verifyDatum(String hashAsHexString, int sourceNodeID) {
		// Always return true, in order for the CB protocol to proceed normally.
		return true;
	}
	
	public void onTimeout() {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: A timeout was fired with initial values " + Arrays.asList(nodeValue).toString());
		
		if(nodeValue.size() < (numberOfNodes - faults))
			System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: WARNING: A timeout was fired, but initial values are missing: " + Arrays.asList(nodeValue).toString());
		
		timeoutFired = true;
		if(!completed)
			beginConsensus();
	}
	
	@Override
	public void process(Message msg) {
		if(!(msg instanceof BroadcastAccept))
			throw new IllegalArgumentException("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: The process() method is not applicable for an argument " + msg.getClass());
	
		BroadcastAccept acc = (BroadcastAccept) msg;	
		
		if(acc.getBroadcastID() == BroadcastID.CB_BROADCAST_ID.getValue()) {
			
			if(!(msg instanceof ConsistentBroadcastAccept))
				throw new IllegalArgumentException("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: The process() method is not applicable for an argument " + msg.getClass());
			
			if(timeoutFired)
				return;

			if(GlobalVariables.HIGH_VERBOSE)
				System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: An initial value was received: <" + acc.getApplicationID() + ", " + acc.getConsensusID()
						+ ", " + acc.getNodeID() + ", " + acc.getValue() + ">");
			
			nodeValue.put(acc.getNodeID(), acc.getValue());
			nodeValueSignatures.put(acc.getNodeID(), ((ConsistentBroadcastAccept) acc).getSignatures());
			
			if(nodeValue.size() == this.numberOfNodes)
			{
				completed = true;
				this.constBroadcast.getCommunicator().cancelTimeout(timeout);
				beginConsensus();
			}
		}
		else {
			if(completedConsensus.containsKey(acc.getConsensusID()))
				return;
			
			BBConsensus cons = activeConsensus.get(acc.getConsensusID());
			if(cons == null){
				cons = new BBConsensus(this, this.id, this.nodeID, acc.getConsensusID(), numberOfNodes);
				activeConsensus.put(acc.getConsensusID(), cons);
			}
			cons.process(acc);
		}
	}
	
	public void beginConsensus() {
		/* Mark the completion of the first phase. */
		this.firstPhaseProcessor.processFirstPhaseCompletion(this.id);

		List<Integer> nodeIDs = this.relBroadcast.getCommunicator().getNodeIDsOfGroup(nodeGroup);
		for (Integer nodeID: nodeIDs) {
			BBConsensus cons = activeConsensus.get(nodeID);
			if (cons == null) {
				cons = new BBConsensus(this, this.id, this.nodeID, nodeID, numberOfNodes);
				activeConsensus.put(nodeID, cons);
			}

			String value = "0";
			if (nodeValue.containsKey(nodeID))
				value = "1";

			cons.start(value);
		}
	}

	@Override
	public void processConsensusResult(int cid, String value)
	{
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: A Consensus instance has terminated: <" + id + ", " + cid + ", " + value + ">");
		
		// Mark the specified consensus as complete.
		completedConsensus.put(cid, value);
		
		// Drop the value, in case it was received during the value dissemination phase.
		if(value.equals("0")) {
			nodeValue.put(cid, "null");
			nodeValueSignatures.remove(cid);
		}
		
		if(completedConsensus.size() == numberOfNodes) {
			// Check if all CB instances have terminated before the barrier.
			if(nodeValue.size() == this.numberOfNodes) {
				List<String> result = new ArrayList<>();
				for(Integer nodeID : nodeValue.keySet())
					result.add(nodeValue.get(nodeID));
				
				this.resultProcessor.processResult(this.id, result);
			}
			else {
				for(Integer consensusID: completedConsensus.keySet()) {

					String consensusValue = completedConsensus.get(consensusID);
					if(consensusValue.equals("1") && nodeValue.containsKey(consensusID) == false) {
						if(GlobalVariables.HIGH_VERBOSE)
							System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: Broadcasting a RecoveryRequestMessage for IC " + id + " and consensus ID " + consensusID);
						
						constBroadcast.getCommunicator().sendGroup(nodeGroup, new RecoveryRequestMessage(id, consensusID));
					}
				}
			}
		}
	}

	@Override
	public IBroadcast getBroadcast() {
		return this.relBroadcast;
	}
	
	public void processRecoveryRequestMessage(RecoveryRequestMessage recoveryRequestMessage, Node source) {
		// Ignore messages from myself.
		if(source.getNodeId() == nodeID)
			return;
		
		if(GlobalVariables.HIGH_VERBOSE) {
			System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: A RecoveryRequestMessage message was received: " + recoveryRequestMessage.toString() + " from Node " + source.getNodeId());
			System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: In processRecoveryRequestMessage():: " + Arrays.asList(nodeValue) + ", " + Arrays.asList(nodeValueSignatures));
		}
		
		int consensusID = recoveryRequestMessage.getConsensusID();
		String value = nodeValue.get(consensusID);
		if(value != null && !value.equals("null")) {
			RecoveryResponseMessage responseMsg = new RecoveryResponseMessage(recoveryRequestMessage.getApplicationID(), consensusID, value);
			responseMsg.setSignatures(nodeValueSignatures.get(consensusID));
			
			if(GlobalVariables.HIGH_VERBOSE)
				System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: Sending a RecoveryResponseMessage " + responseMsg.toString() + " to Node " + source.getNodeId());
			
			constBroadcast.getCommunicator().send(source, responseMsg);
		}
	}
	
	public void processRecoveryResponseMessage(RecoveryResponseMessage recoveryResponseMessage, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: A RecoveryResponseMessage message was received: " + recoveryResponseMessage.toString() + " from Node " + source.getNodeId());
				
		// Ignore the message if the corresponding inputValue was received.
		int consensusID = recoveryResponseMessage.getConsensusID();
		if(nodeValue.containsKey(consensusID))
			return;
			
		CryptographyModule cryptoModule = ((ConsistentBroadcast) constBroadcast).getCryptoModule();
		AbstractCommunicator communicator = constBroadcast.getCommunicator();
		String content = recoveryResponseMessage.getContent(defaultConsensusID);
		Key key = null;
		
		for(Integer currentNodeID: recoveryResponseMessage.getNodeIDsInSignature()) {
			byte[] signature = null;
			
			if(cryptoModule instanceof DigitalSignatureCryptographyModule) {
				key = communicator.getOtherNode(currentNodeID).getPublicKey();
				signature = recoveryResponseMessage.getSignature(currentNodeID);
			}
			else {
				key = communicator.getCurrentNode().getSymmetricKey(currentNodeID);
				signature = recoveryResponseMessage.getSignature(currentNodeID, consensusID, cryptoModule.getDigestLength());
			}

			if(!cryptoModule.verify(content, key, signature)) {
				System.err.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: The RecoveryResponseMessage: " + recoveryResponseMessage.toString()
						+ " received from Node " + source.getNodeId() + " couldn't be verified!");
				return;
			}
		}
		
		// Accept the new value and check if the IC vector is now complete.
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBInteractiveConsistency, Node: " + nodeID + ", ICID: " + id + "]: Accepting the value " + recoveryResponseMessage.getValue() + " for consensus ID " + consensusID);
		
		nodeValue.put(consensusID, recoveryResponseMessage.getValue());
		nodeValueSignatures.put(consensusID, recoveryResponseMessage.getSignatures());
		
		if(nodeValue.size() == this.numberOfNodes) {
			List<String> result = new ArrayList<>();
			
			for(Integer nodeID : nodeValue.keySet())
				result.add(nodeValue.get(nodeID));
			
			this.resultProcessor.processResult(this.id, result);
		}
	}
}
