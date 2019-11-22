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
package gr.uoa.di.dsg.broadcast.consistent;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.broadcast.reliable.ReliableBroadcastWithSignatures;
import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.crypto.CryptographyModule;
import gr.uoa.di.dsg.ic.ApplicationGetter;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This class enhances the functionality provided by the {@link ConsistentBroadcast} protocol,
 * by satisfying the <i>totality</i> property. The totality property ensures that if a correct
 * node delivers a specific message, then eventually, all correct nodes will receive the specific
 * message as well.
 * 
 * The {@link ReliableBroadcastWithSignatures} protocol modifies the third step of the protocol. More specifically,
 * the node broadcasts a message only upon its initial receipt. Afterwards, it only process and stores
 * the incoming messages. The node delivers a message, only after it has received and verified the same
 * message by <i>2t + 1</i> different nodes.
 * 
 * Furthermore, the protocol's execution requires some form of authenticity, in order for all exchanged
 * messages to be verified. For example, the current implementation supports either digital signatures, or
 * authenticators.
 * 
 * @author stathis
 *
 */

public class ConsistentBroadcast extends GenericBroadcastWithSignatures {
	
	/**
	 * Creates an instance of the {@link ConsistentBroadcast} protocol that
	 * uses digital signatures, in order to authenticate all exchanged messages.
	 */
	public ConsistentBroadcast() {
		super();
	}
	
	/**
	 * Creates an instance of the {@link ConsistentBroadcast} protocol that
	 * uses the specified cryptography module, in order to authenticate all
	 * exchanged messages.
	 * 
	 * @param cryptoModule the cryptography module to be used by the protocol.
	 */
	public ConsistentBroadcast(CryptographyModule cryptoModule) {
		super(cryptoModule);
	}
	
	@Override
	protected CBSendMessage getSendMessage(String icid, int cid, int pid, int bid, String value) {
		return new CBSendMessage(icid, cid, pid, bid, value);
	}

	@Override
	protected CBEchoMessage getEchoMessage(String icid, int cid, int pid, int bid, String value) {
		return new CBEchoMessage(icid, cid, pid, bid, value);
	}

	@Override
	protected CBFinalMessage getFinalMessage(String icid, int cid, int pid, int bid, String value) {
		return new CBFinalMessage(icid, cid, pid, bid, value);
	}
	
	@Override
	public void initialize(AbstractCommunicator comm, ApplicationGetter appGetter, String nodeGroup, int numNodes) {
		comm.registerMessage(BrodacastWithSignaturesMessageType.CBSEND.getValue(), (byte[] data) -> CBSendMessage.deserialize(data), (Message msg, Node source) -> onCBSend((CBSendMessage) msg, source));
		comm.registerMessage(BrodacastWithSignaturesMessageType.CBECHO.getValue(), (byte[] data) -> CBEchoMessage.deserialize(data), (Message msg, Node source) -> onCBEcho((CBEchoMessage) msg, source));
		comm.registerMessage(BrodacastWithSignaturesMessageType.CBFINAL.getValue(), (byte[] data) -> CBFinalMessage.deserialize(data), (Message msg, Node source) -> onCBFinal((CBFinalMessage) msg, source));
		
		this.communicator = comm;
		this.applicationGetter = appGetter;
		this.nodeGroup = nodeGroup;
		this.totalNodes = numNodes;
		
		/* Initialize all data structures. */
		this.activeBroadcasts = new HashMap<>();
		this.completedBroadcasts = new HashSet<>();
		
		/* Specify the maximum number of faults. */
		this.faults = (int) Math.floor((totalNodes - 1) / 3.0);
	}
	
	private void onCBSend(CBSendMessage message, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[" + this.getClass().getName() + ", Node: " + communicator.getCurrentNode().getNodeId() + "]: A CBSendMessage was received: " + message.toString() + " from Node " + source.getNodeId());

		/* Verify that the datum is correct. The check if the datum feature
		 * is supported is performed inside the verifyDatum method. */
		if(!applicationGetter.getApp(message.getApplicationID()).verifyDatum(message.getValue(), message.getNodeID())) {
			System.err.println("[" + communicator.getCurrentNode().getNodeId() + "]: The datum " + message.getValue() + " received by node " + source.getNodeId() + " is not correct!");
			return;
		}
				
		process(message, source);
	}
	
	private void onCBEcho(CBEchoMessage message, Node source) {
		process(message, source);
	}
	
	/**
	 * The handler of the {@link CBFinalMessage}. These messages are exchanged during the third
	 * phase of the {@link ConsistentBroadcast} protocol. Upon the receipt of a message, the node
	 * stores it and then, the upper layer of the application is notified about the final value
	 * delivered by the protocol.
	 * 
	 * @param message the message sent during the third round of the protocol.
	 * @param source the sender node that broadcasts the specified message.
	 */
	public void onCBFinal(CBFinalMessage message, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[" + this.getClass().getName() + ", Node: " + communicator.getCurrentNode().getNodeId() + "]: A CBFinalMessage was received: " + message.toString() + " from Node " + source.getNodeId());
		
		/* Verify that the processID is correct. */
		if(message.getNodeID() != source.getNodeId()) {
			System.err.println("[Node: " + communicator.getCurrentNode().getNodeId() + "]: received an invalid message: " + message.toString() + " from Node: " + source.getNodeId());
			return;
		}
		
		/* Verify the validity of the received message. */
		if(message.getTotalSignatures() < (totalNodes - faults)) {
			System.err.println("[" + communicator.getCurrentNode().getNodeId() + "]: The CBFinalMessage: " + message.toString()
					+ " received from Node " + source.getNodeId() + " is not supported by enough signatures!");
			return;
		}
		
		/* Create a unique ID, based on the message's parameters. */
		String UUID = message.getUUID();
		if(completedBroadcasts.contains(UUID))
			return;
		
		String content = message.getUUID() + message.getValue();
		
		/* Verify the validity of the received message. */
		this.verifySignaturesAsync(message, source, content, communicator.getCurrentNode().getNodeId(), (msg, src) -> processCBFinal((CBFinalMessage) msg, src));
	}
	
	private void processCBFinal(CBFinalMessage message, Node source) {
		/* Create a unique ID, based on the message's parameters. */
		String UUID = message.getUUID();
		if(completedBroadcasts.contains(UUID))
			return;
		
		/* Mark the broadcast as completed and remove all associated information. */
		this.completedBroadcasts.add(UUID);
		this.activeBroadcasts.remove(UUID);
		
		/* Make an up-call with the delivered message. */
		BroadcastAccept deliverMessage = new ConsistentBroadcastAccept(message.getApplicationID(), message.getConsensusID(), message.getNodeID() , message.getBroadcastID(), message.getValue(), message.getSignatures());
		applicationGetter.getApp(message.getApplicationID()).process(deliverMessage);		
	}
}
