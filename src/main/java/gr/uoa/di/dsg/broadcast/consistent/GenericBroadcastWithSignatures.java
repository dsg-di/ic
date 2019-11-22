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

import gr.uoa.di.dsg.broadcast.BroadcastMessage;
import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.communicator.AsynchronousTaskResultMessage;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.crypto.CryptographyModule;
import gr.uoa.di.dsg.crypto.DigitalSignatureCryptographyModule;
import gr.uoa.di.dsg.ic.ApplicationGetter;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GenericBroadcastWithSignatures implements IBroadcast {
	/**
	 * The Communicator instance of this class.
	 */
	protected transient AbstractCommunicator communicator = null;
	
	/**
	 * The Application instance that uses this broadcast.
	 */
	protected transient ApplicationGetter applicationGetter = null;
	
	/**
	 * A map that stores all necessary information for each broadcast.
	 */
	protected Map<String, Map<String, CBandRBSBroadcastInfo>> activeBroadcasts = null;
	
	/**
	 * The set of all completed broadcasts.
	 */
	protected Set<String> completedBroadcasts = null;
	
	/**
	 * The total number of nodes in the system.
	 */
	protected int totalNodes;
	
	/**
	 * The maximum number of faults.
	 */
	protected int faults;
	
	/**
	 * The cryptography module of this broadcast.
	 */
	private transient final CryptographyModule cryptoModule;
	
	/** The communication group that this broadcast module belongs. */
	protected String nodeGroup = null;

	/**
	 * Returns the number of nodes participating in the protocol.
	 * @return the number of nodes participating in the protocol.
	 */
	public int getTotalNodes() {
		return totalNodes;
	}

	/**
	 * Returns the communicator instance that is responsible for transmitting messages.
	 * @return the communicator instance that is responsible for transmitting messages.
	 */
	public AbstractCommunicator getCommunicator() {
		return communicator;
	}

	/**
	 * Returns the Application instance used by this broadcast.
	 * @return the Application instance used by this broadcast.
	 */
	public ApplicationGetter getApplicationGetter() {
		return applicationGetter;
	}
	
	/**
	 * Returns the number of faults tolerated by the application.
	 * @return the number of faults tolerated by the application.
	 */
	public int getFaults() {
		return faults;
	}

	/**
	 * Returns the cryptography module used by the application.
	 * @return the cryptography module used by the application.
	 */
	public CryptographyModule getCryptoModule() {
		return cryptoModule;
	}
	
	/**
	 * Creates an instance of the {@link GenericBroadcastWithSignatures} protocol that
	 * uses digital signatures, in order to authenticate all exchanged messages.
	 */
	public GenericBroadcastWithSignatures() {
		this.cryptoModule = new DigitalSignatureCryptographyModule();
	}
	
	/**
	 * Creates an instance of the {@link GenericBroadcastWithSignatures} protocol that
	 * uses the specified cryptography module, in order to authenticate all
	 * exchanged messages.
	 * 
	 * @param cryptoModule the cryptography module to be used by the protocol.
	 */
	public GenericBroadcastWithSignatures(CryptographyModule cryptoModule) {
		this.cryptoModule = cryptoModule;
	}
	
	abstract protected CBSendMessage getSendMessage(String icid, int cid, int pid, int bid, String value);
	abstract protected CBEchoMessage getEchoMessage(String icid, int cid, int pid, int bid, String value);
	abstract protected CBFinalMessage getFinalMessage(String icid, int cid, int pid, int bid, String value);
	
	@Override
	public void broadcast(String icid, int cid, int pid, int bid, String value) {
		CBSendMessage msg = getSendMessage(icid, cid, pid, bid, value);
		
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[" + this.getClass().getName() + ", Node: " + pid + "]: Starting a broadcast for " + msg.toString());
		
		// Mark my broadcast as active.
		String UUID = msg.getUUID();
		Map<String, CBandRBSBroadcastInfo> perValueMap = new HashMap<>();
		perValueMap.put(value, new CBandRBSBroadcastInfo());
		activeBroadcasts.put(UUID, perValueMap);
		
		this.communicator.sendGroup(nodeGroup, msg);
	}
	
	protected CBandRBSBroadcastInfo getCBandRBSBroadcastInfo(String UUID, String value) {
		Map<String, CBandRBSBroadcastInfo> perValueMap = activeBroadcasts.get(UUID);
		if(perValueMap == null) {
			perValueMap = new HashMap<>();
			perValueMap.put(value, new CBandRBSBroadcastInfo());
			activeBroadcasts.put(UUID, perValueMap);
		}
		
		CBandRBSBroadcastInfo broadcastInfo = perValueMap.get(value);
		if(broadcastInfo == null) {
			broadcastInfo = new CBandRBSBroadcastInfo();
			perValueMap.put(value, broadcastInfo);
		}
		
		return broadcastInfo;
	}
	
	protected void signMessageAsync(BroadcastMessage msg, Node source, String sourceAlias, String content, Key key, SignatureResultProcessor processor) {
		if(!GlobalVariables.ENABLE_ASYNC_WORK) {
			CBEchoMessage echoMessage = (CBEchoMessage) msg;
			echoMessage.setSignature(cryptoModule.sign(content, key));
			processor.processResult(echoMessage, source);
		}
		else {
			long requestTime = System.currentTimeMillis();
			int currentMessageOrder = communicator.getCurrentMessageOrder();
			communicator.submitBackgroundTask( () -> 
			{
				CBEchoMessage echoMessage = (CBEchoMessage) msg;
				echoMessage.setSignature(cryptoModule.sign(content, key));
				
				communicator.inputEnqueue(communicator.getCurrentNode(), 
					new AsynchronousTaskResultMessage("signMessage", "", Message.DEFAULT_SUBJECT, requestTime, currentMessageOrder,
							() -> {
								processor.processResult(echoMessage, source);
							})
				);
			});
		}
	}
	
	protected void signMessageAuthenticatorAsync(BroadcastMessage msg, Node source, String content, SignatureResultProcessor processor) {
		if(!GlobalVariables.ENABLE_ASYNC_WORK) {
			CBEchoMessage echoMessage = (CBEchoMessage) msg;

			List<Integer> nodeIDs = new ArrayList<>(totalNodes);
			List<byte[]> signatures = new ArrayList<>(totalNodes);
			
			for(int i = 0; i < totalNodes; ++i) {
				Key key = this.communicator.getCurrentNode().getSymmetricKey(i);
				nodeIDs.add(i);
				signatures.add(cryptoModule.sign(content, key));
			}
			
			try {
				echoMessage.setSignature(nodeIDs, signatures);
			}
			catch (IOException ex) {
				System.err.println("An IOException was caught: " + ex.getMessage());
				ex.printStackTrace();
			}
			
			processor.processResult(echoMessage, source);
		}
		else {
			long requestTime = System.currentTimeMillis();
			int currentMessageOrder = communicator.getCurrentMessageOrder();
			communicator.submitBackgroundTask( () -> 
			{
				CBEchoMessage echoMessage = (CBEchoMessage) msg;

				List<Integer> nodeIDs = new ArrayList<>(totalNodes);
				List<byte[]> signatures = new ArrayList<>(totalNodes);
				
				for(int i = 0; i < totalNodes; ++i) {
					nodeIDs.add(i);
					signatures.add(cryptoModule.sign(content, this.communicator.getCurrentNode().getSymmetricKey(i)));
				}
				
				try {
					echoMessage.setSignature(nodeIDs, signatures);
				}
				catch (IOException ex) {
					System.err.println("An IOException was caught: " + ex.getMessage());
					ex.printStackTrace();
				}
				
				communicator.inputEnqueue(communicator.getCurrentNode(), 
					new AsynchronousTaskResultMessage("signMessage", "", Message.DEFAULT_SUBJECT, requestTime, currentMessageOrder,
							() -> {
								processor.processResult(echoMessage, source);
							})
				);
			});
		}
	}
	
	protected void verifySignaturesAsync(CBFinalMessage message, Node source, String content, int selfID, VerificationResultProcessor processor) {
		if(!GlobalVariables.ENABLE_ASYNC_WORK) {
			Key key = null;
			
			//System.out.println("Trying to verify message: " + message.toString());
			for(Integer currentNodeID: message.getNodeIDsInSignature()) {
				byte[] signature = null;
				
				if(cryptoModule instanceof DigitalSignatureCryptographyModule) {
					key = communicator.getOtherNode(currentNodeID).getPublicKey();
					signature = message.getSignature(currentNodeID);
				}
				else {
					key = this.communicator.getCurrentNode().getSymmetricKey(currentNodeID);
					signature = message.getSignature(currentNodeID, selfID, cryptoModule.getDigestLength());
				}

				if(!cryptoModule.verify(content, key, signature)) {
					System.err.println("[" + communicator.getCurrentNode().getNodeId() + "]: The CBFinalMessage: " + message.toString()
							+ " received from Node " + source.getNodeId() + " couldn't be verified!");
					return;
				}
			}
			//System.out.println("Message: " + message.toString() + " verified!");
			
			processor.processResult(message, source);
		}
		else {
			long requestTime = System.currentTimeMillis();
			int currentMessageOrder = communicator.getCurrentMessageOrder();
			communicator.submitBackgroundTask( () -> 
			{
				Key key = null;
				boolean isValid = true;
				
				//System.out.println("Trying to verify message: " + message.toString());
				for(Integer currentNodeID: message.getNodeIDsInSignature()) {
					byte[] signature = null;
					
					if(cryptoModule instanceof DigitalSignatureCryptographyModule) {
						key = communicator.getOtherNode(currentNodeID).getPublicKey();
						signature = message.getSignature(currentNodeID);
					}
					else {
						key = this.communicator.getCurrentNode().getSymmetricKey(currentNodeID);
						signature = message.getSignature(currentNodeID, selfID, cryptoModule.getDigestLength());
					}

					if(!cryptoModule.verify(content, key, signature)) {
						isValid = false;
						break;
					}
				}
				//System.out.println("Message: " + message.toString() + " verified!");
				
				if(!isValid) {
					communicator.inputEnqueue(communicator.getCurrentNode(), 
							new AsynchronousTaskResultMessage("verifySignatures", "", Message.DEFAULT_SUBJECT, requestTime, currentMessageOrder,
									() -> {
										//if(GlobalVariables.HIGH_VERBOSE)
										System.err.println("[" + communicator.getCurrentNode().getNodeId() + "]: The CBFinalMessage: " + message.toString()
											+ " received from Node " + source.getNodeId() + " couldn't be verified!");
										return;
									})
					);
				}
				else {
					communicator.inputEnqueue(communicator.getCurrentNode(), 
							new AsynchronousTaskResultMessage("verifySignatures", "", Message.DEFAULT_SUBJECT, requestTime, currentMessageOrder,
									() -> {
										processor.processResult(message, source);
									})
					);
				}
			});
		}
		
		return;
	}
	
	private void verifySignatureAsync(BroadcastMessage message, Node source, String content, Key key, byte[] signature, VerificationResultProcessor processor) {
		if(!GlobalVariables.ENABLE_ASYNC_WORK) {
			if(!cryptoModule.verify(content, key, signature)) {
				System.err.println("[" + communicator.getCurrentNode().getNodeId() + "]: The CBEchoMessage: " + message.toString()
					+ " received from Node " + source.getNodeId() + " couldn't be verified!");
				
				return;
			}
			
			processor.processResult(message, source);
		}
		else {
			long requestTime = System.currentTimeMillis();
			int currentMessageOrder = communicator.getCurrentMessageOrder();
			communicator.submitBackgroundTask( () -> 
			{
				final boolean isValid = cryptoModule.verify(content, key, signature);
				if(!isValid) {
					communicator.inputEnqueue(communicator.getCurrentNode(), 
							new AsynchronousTaskResultMessage("verifySignature", "", Message.DEFAULT_SUBJECT, requestTime, currentMessageOrder,
									() -> {
										System.err.println("[" + communicator.getCurrentNode().getNodeId() + "]: The CBEchoMessage: " + message.toString()
											+ " received from Node " + source.getNodeId() + " couldn't be verified!");
										return;
									})
					);
				}
				else {
					communicator.inputEnqueue(communicator.getCurrentNode(), 
							new AsynchronousTaskResultMessage("verifySignature", "", Message.DEFAULT_SUBJECT, requestTime, currentMessageOrder,
									() -> {
										processor.processResult(message, source);
									})
					);
				}
			});
		}
	}
	
	/**
	 * The handler of the {@link CBSendMessage}. These messages are exchanged during the first phase
	 * of both protocols. That message involves the private value of the sender node. Upon the receipt
	 * of the message, the recipient node responds by unicasting a {@link CBEchoMessage} to the sender
	 * node, acknowledging that the node's private value has been received.
	 * 
	 * @param message the message sent during the first round of the protocol.
	 * @param source the sender node that broadcasts the specified message.
	 */
	protected void process(CBSendMessage message, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[" + this.getClass().getName() + ", Node: " + communicator.getCurrentNode().getNodeId() + "]: A CBSendMessage was received: " + message.toString() + " from Node " + source.getNodeId());
		
		/* Verify that the processID is correct. */
		if(message.getNodeID() != source.getNodeId()) {
			System.err.println("(0): The node " + communicator.getCurrentNode().getNodeId() + " received an invalid message: " + message.toString() + " from Node: " + source.getNodeId());
			System.exit(-1);
		}
		
		/* Get the stored information for the specified broadcast. */
		String UUID = message.getUUID();
		if(completedBroadcasts.contains(UUID))
			return;
		
		/* If the broadcast is mine, endorse without any more checks.
		 * Else, verify we have not endorsed any other value.  */
		CBandRBSBroadcastInfo cbInfo = null;
		if(message.getNodeID() != communicator.getCurrentNode().getNodeId()) {
			Map<String, CBandRBSBroadcastInfo> perValueMap = activeBroadcasts.get(UUID);
			if(perValueMap == null) {
				perValueMap = new HashMap<>();
				cbInfo = new CBandRBSBroadcastInfo();
				perValueMap.put(message.getValue(), cbInfo);
				activeBroadcasts.put(UUID, perValueMap);
			}
			else
				return;
		}
		else {
			cbInfo = getCBandRBSBroadcastInfo(UUID, message.getValue());
		}
		
		/* Update the current state for this broadcast. */
		cbInfo.currentState = 2;
		
		/* Create the reply message. */
		CBEchoMessage echoMessage = getEchoMessage(message.getApplicationID(), message.getConsensusID(), message.getNodeID(), message.getBroadcastID(), message.getValue());
		
		/* Compute the message's signature and send the reply. */
		String content = echoMessage.getUUID() + echoMessage.getValue();
		
		if(cryptoModule instanceof DigitalSignatureCryptographyModule) {
			Node current = this.communicator.getCurrentNode();
			this.signMessageAsync(echoMessage, source, String.valueOf(current.getNodeId()), content, current.getPrivateKey(), (newMessage, src) -> processCBSend((CBEchoMessage) newMessage, src));
		}
		else
			this.signMessageAuthenticatorAsync(echoMessage, source, content, (newMessage, src) -> processCBSend((CBEchoMessage) newMessage, src));
	}
	
	protected void processCBSend(CBEchoMessage echoMessage, Node source) {	
		this.communicator.send(source, echoMessage);
	}
	
	/**
	 * The handler of the {@link CBEchoMessage}. These messages are exchanged during the second phase
	 * of both protocols. The message must be verified, before being processed. Once the appropriate
	 * number of {@link CBEchoMessage} messages has been received, the node proceeds to the next phase
	 * of the protocol by broadcasting a {@link CBFinalMessage}.
	 * 
	 * @param message the message sent during the second round of the protocol.
	 * @param source the sender node that broadcasts the specified message.
	 */
	protected void process(CBEchoMessage message, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[" + this.getClass().getName() + ", Node: " + communicator.getCurrentNode().getNodeId() + "]: A CBEchoMessage was received: " + message.toString() + " from Node " + source.getNodeId());
		
		if(message.getNodeID() != communicator.getCurrentNode().getNodeId()) {
			System.err.println("[" + communicator.getCurrentNode().getNodeId() + "]: The CBEchoMessage: " + message.toString()
					+ " received from Node " + source.getNodeId() + " contains an invalid nodedID: " + message.getNodeID());
			return;
		}
		
		/* Create a unique ID, based on the message's parameters. */
		String UUID = message.getUUID();
		if(completedBroadcasts.contains(UUID))
			return;
		
		String content = message.getUUID() + message.getValue();
		Key key = null;
		byte[] signature = null;
		
		//System.out.println("Trying to verify message: " + message.toString());
		if(cryptoModule instanceof DigitalSignatureCryptographyModule) {
			key = communicator.getOtherNode(source.getNodeId()).getPublicKey();
			signature = message.getSignature();
		}
		else {
			key = this.communicator.getCurrentNode().getSymmetricKey(source.getNodeId());
			signature = message.getSignature(this.communicator.getCurrentNode().getNodeId(), cryptoModule.getDigestLength());
		}
		
		this.verifySignatureAsync(message, source, content, key, signature, (msg, src) -> processCBEcho((CBEchoMessage) msg, src));		
	}
	
	protected void processCBEcho(CBEchoMessage message, Node source) {
		String UUID = message.getUUID();
		
		/* Get the stored information for the specified broadcast. */
		CBandRBSBroadcastInfo cbInfo = getCBandRBSBroadcastInfo(UUID, message.getValue());
		if(cbInfo.currentState <= 2) {
			/* Store the first message from each node. */
			if(cbInfo.incomingSignatures.containsKey(source.getNodeId()) == false)
				cbInfo.incomingSignatures.put(source.getNodeId(), message.getSignature());
			
			/* Check if the proper number of messages have been received. */
			if(cbInfo.incomingSignatures.size() == (totalNodes - faults)) {
				/* Compute the message's signatures and send the CBFinalMessage. */
				CBFinalMessage finalMessage = getFinalMessage(message.getApplicationID(), message.getConsensusID(), this.getCommunicator().getCurrentNode().getNodeId(), message.getBroadcastID(), message.getValue());
				
				for(Integer owner: cbInfo.incomingSignatures.keySet())
					finalMessage.addSignature(owner, cbInfo.incomingSignatures.get(owner));
				
				/* Remove all messages associated with this UUID. */
				cbInfo.incomingSignatures.clear();
				
				/* Update the current state for this broadcast. */
				cbInfo.currentState = 3;
				
				/* Increase the finalMessages counter, in order to avoid a 2nd broadcast. */
				++(cbInfo.totalFinalMessages);
				
				/* Send a CBFinal message to all nodes of the system. */
				this.communicator.sendGroup(nodeGroup, finalMessage);
			}
		}
	}
}
