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
package gr.uoa.di.dsg.broadcast.reliable;

import java.util.HashMap;
import java.util.HashSet;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.broadcast.consistent.CBandRBSBroadcastInfo;
import gr.uoa.di.dsg.broadcast.consistent.CBEchoMessage;
import gr.uoa.di.dsg.broadcast.consistent.CBFinalMessage;
import gr.uoa.di.dsg.broadcast.consistent.BrodacastWithSignaturesMessageType;
import gr.uoa.di.dsg.broadcast.consistent.CBSendMessage;
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcast;
import gr.uoa.di.dsg.broadcast.consistent.GenericBroadcastWithSignatures;
import gr.uoa.di.dsg.broadcast.consistent.RBSEchoMessage;
import gr.uoa.di.dsg.broadcast.consistent.RBSFinalMessage;
import gr.uoa.di.dsg.broadcast.consistent.RBSSendMessage;
import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.crypto.CryptographyModule;
import gr.uoa.di.dsg.ic.ApplicationGetter;
import gr.uoa.di.dsg.utils.GlobalVariables;

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

public class ReliableBroadcastWithSignatures extends GenericBroadcastWithSignatures {
	
	/**
	 * Creates an instance of the {@link ReliableBroadcastWithSignatures} protocol that
	 * uses digital signatures, in order to authenticate all exchanged messages.
	 */
	public ReliableBroadcastWithSignatures() {
		super();
	}
	
	@Override
	protected CBSendMessage getSendMessage(String icid, int cid, int pid, int bid, String value) {
		return new RBSSendMessage(icid, cid, pid, bid, value);
	}

	@Override
	protected CBEchoMessage getEchoMessage(String icid, int cid, int pid, int bid, String value) {
		return new RBSEchoMessage(icid, cid, pid, bid, value);
	}

	@Override
	protected CBFinalMessage getFinalMessage(String icid, int cid, int pid, int bid, String value) {
		return new RBSFinalMessage(icid, cid, pid, bid, value);
	}
	
	@Override
	public void initialize(AbstractCommunicator comm, ApplicationGetter appGetter, String nodeGroup, int numNodes) {
		/* Notice: We keep the same class and wire format, by reusing the CBxxxMessage classes.
		 * However, the handlers are distinct from CB. */
		comm.registerMessage(BrodacastWithSignaturesMessageType.RBSSEND.getValue(), (byte[] data) -> RBSSendMessage.deserialize(data), (Message msg, Node source) -> onRBSSend((CBSendMessage) msg, source));
		comm.registerMessage(BrodacastWithSignaturesMessageType.RBSECHO.getValue(), (byte[] data) -> RBSEchoMessage.deserialize(data), (Message msg, Node source) -> onRBSEcho((CBEchoMessage) msg, source));
		comm.registerMessage(BrodacastWithSignaturesMessageType.RBSFINAL.getValue(), (byte[] data) -> RBSFinalMessage.deserialize(data), (Message msg, Node source) -> onRBSFinal((CBFinalMessage) msg, source));
		
		this.communicator = comm;
		this.applicationGetter = appGetter;
		this.nodeGroup = nodeGroup;
		this.totalNodes = numNodes;
		
		/* Initialize all data structures. */
		this.activeBroadcasts = new HashMap<>();
		this.completedBroadcasts = new HashSet<>();
		
		/* Update the maximum number of faults. */
		this.faults = (int) Math.floor((totalNodes - 1) / 3.0);
	}
	
	/**
	 * Creates an instance of the {@link ReliableBroadcastWithSignatures} protocol that
	 * uses the specified cryptography module, in order to authenticate all
	 * exchanged messages.
	 *
	 * @param cryptoModule the cryptography module to be used by the protocol.
	 */
	public ReliableBroadcastWithSignatures(CryptographyModule cryptoModule) {
		super(cryptoModule);
	}
	
	private void onRBSSend(CBSendMessage message, Node source) {
		process(message, source);
	}
	
	private void onRBSEcho(CBEchoMessage message, Node source) {
		process(message, source);
	}
	
	/**
	 * The handler of the {@link CBFinalMessage}. These messages are exchanged during the
	 * third phase of the {@link ReliableBroadcastWithSignatures} protocol. Upon the first receipt of a
	 * specific message, the node stores it and broadcasts it to the rest nodes of the system.
	 * Once a specific message has been received an appropriate number of times, the protocol notifies
	 * the upper layer of the application about the final value delivered by the protocol.
	 * 
	 * @param message the message sent during the third round of the protocol.
	 * @param source the sender node that broadcasts the specified message.
	 */
	private void onRBSFinal(CBFinalMessage message, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[" + this.getClass().getName() + ", Node: " + communicator.getCurrentNode().getNodeId() + "]: A RBSFinalMessage was received: " + message.toString() + " from Node " + source.getNodeId());
		
		/* Verify the validity of the received message. */
		if(message.getTotalSignatures() < (totalNodes - faults)) {
			System.err.println("[" + communicator.getCurrentNode().getNodeId() + "]: The RBSFinalMessage: " + message.toString()
					+ " received from Node " + source.getNodeId() + " is not supported by enough signatures!");
			return;
		}
		
		/* Create a unique ID, based on the message's parameters. */
		String UUID = message.getUUID();
		if(completedBroadcasts.contains(UUID))
			return;
		
		String content = message.getUUID() + message.getValue();
		
		/* Verify the validity of the received message. */
		this.verifySignaturesAsync(message, source, content, communicator.getCurrentNode().getNodeId(), (msg, src) -> processRBSFinal((RBSFinalMessage) msg, src));
	}
	
	private void processRBSFinal(RBSFinalMessage message, Node source) {
		/* Create a unique ID, based on the message's parameters. */
		String UUID = message.getUUID();
		if(completedBroadcasts.contains(UUID))
			return;
		
		/* Get the stored information for the specified broadcast. */
		CBandRBSBroadcastInfo cbInfo = getCBandRBSBroadcastInfo(UUID, message.getValue());
		
		/* Multicast a CBFinalMessage to all nodes of the system, only once. */
		if((++cbInfo.totalFinalMessages) == 1)
			communicator.sendGroup(nodeGroup, message);
		
		if (cbInfo.totalFinalMessages == (this.getTotalNodes() - this.getFaults())) {
			/* Mark the broadcast as completed and remove all associated information. */
			this.completedBroadcasts.add(UUID);
			this.activeBroadcasts.remove(UUID);
			
			/* Make an up-call with the delivered message. */
			BroadcastAccept deliverMessage = new BroadcastAccept(message.getApplicationID(), message.getConsensusID(), message.getNodeID(), message.getBroadcastID(), message.getValue());
			this.getApplicationGetter().getApp(message.getApplicationID()).process(deliverMessage);
		}
	}
}
