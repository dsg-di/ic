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

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcast;
import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.consensus.bracha.BBConsensus;
import gr.uoa.di.dsg.crypto.CryptographyModule;
import gr.uoa.di.dsg.crypto.DigitalSignatureCryptographyModule;
import gr.uoa.di.dsg.ic.ICFirstPhase;
import gr.uoa.di.dsg.ic.ICResult;
import gr.uoa.di.dsg.ic.recovery.ICChunkRecoveryMessage;
import gr.uoa.di.dsg.ic.recovery.RecoveryRequestMessage;
import gr.uoa.di.dsg.ic.recovery.RecoveryResponseMessage;
import gr.uoa.di.dsg.utils.BroadcastID;
import gr.uoa.di.dsg.utils.GlobalUtils;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BBInteractiveConsistencyDatum extends BBInteractiveConsistency {
	
	/** Data structures to support a datum, instead of a single value. */
	private GetNextChunkProcessor getProcessor = null;
	private StoreChunkProcessor storeProcessor = null;
	private RecoveryChunkProcessor recoveryProcessor = null;
	
	private Map<Integer, Map<Integer, byte[]>> chunkDataPerNode = new HashMap<>();
	private HashMap<Integer, Integer> nextChunkIDPerNode = new HashMap<>();
	private HashMap<Integer, Integer> totalChunksPerNode = new HashMap<>();
	private Map<Integer, MessageDigest> digestMap = new HashMap<>();
	private Map<Integer, byte[]> finalDigests = new HashMap<>();
	
	/** Data structures to support the datum's recovery.
	  * The key in the following maps has the form "consensusID:nodeID". */
	private Map<String, Integer> nextChunkIDRecovery = new HashMap<>();
	private Map<String, Integer> totalChunksRecovery = new HashMap<>();
	private Map<String, MessageDigest> digestMapRecovery = new HashMap<>();
	private Map<String, Map<Integer, byte[]>> nodeValueSignaturesRecovery = new HashMap<>();
	private Map<String, Map<Integer, byte[]>> chunkDataPerNodeRecovery = new HashMap<>();
	
	public BBInteractiveConsistencyDatum(String id, int nodeID, int numNodes, IBroadcast relBroadcast, IBroadcast constBroadcast, ICResult resultProcessor, ICFirstPhase firstPhaseProcessor, String nodeGroup, GetNextChunkProcessor getProcessor, StoreChunkProcessor storeProcessor, RecoveryChunkProcessor recoveryProcessor) {
		super(id, nodeID, numNodes, relBroadcast, constBroadcast, resultProcessor, firstPhaseProcessor, nodeGroup);
		
		this.getProcessor = getProcessor;
		this.storeProcessor = storeProcessor;
		this.recoveryProcessor = recoveryProcessor;
	}
	
	private int getNextChunkIDPerNode(int nodeId) {
		return nextChunkIDPerNode.getOrDefault(nodeId, 0);
	}
	
	private void incrNextChunkIDPerNode(int nodeId) {
		nextChunkIDPerNode.put(nodeId, nextChunkIDPerNode.getOrDefault(nodeId, 0) + 1);
	}
	
	private int getTotalChunksPerNode(int nodeId) {
		return totalChunksPerNode.getOrDefault(nodeId, -1);
	}

	private void setTotalChunksPerNode(int nodeId, int value) {
		totalChunksPerNode.put(nodeId, value);
	}

	@Override
	public void start(int pid, String value) {
		throw new RuntimeException("This operation is not supported by a BBInteractiveConsistencyDatum application!");
	}
	
	@Override
	public void start() {
		if (GlobalVariables.HIGH_VERBOSE)
			System.out.println("TIMEOUT = " + GlobalVariables.TIMEOUT);
		
		this.timeout = constBroadcast.getCommunicator().setTimeout(GlobalVariables.TIMEOUT, () -> onTimeout());
		getProcessor.getNextChunk(getNextChunkIDPerNode(nodeID));
	}
	
	private void datumCleanupBeforeConsensus() {
		chunkDataPerNode.clear();
		digestMap.clear();
	}
	
	@Override
	public void beginConsensus() {
		// Remove any remains from the datum expansion during the value dissemination phase.
		datumCleanupBeforeConsensus();
		
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
			System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: A Consensus instance has terminated: <" + id + ", " + cid + ", " + value + ">");
		
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
							System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Broadcasting a RecoveryRequestMessage for IC " + id + " and consensus ID " + consensusID);
						
						// Remove any temporary digests that (may) have been calculated during the value dissemination phase.
						finalDigests.remove(consensusID);						
						constBroadcast.getCommunicator().sendGroup(nodeGroup, new RecoveryRequestMessage(id, consensusID));
					}
				}
			}
		}
	}
	
	@Override
	public void processDatumChunk(ICChunkMessage icChunkMessage) {
		// Check if the value dissemination phase has been completed.
		if(completed || timeoutFired)
			return;
		
		// Send the current datum chunk to all nodes.
		this.constBroadcast.getCommunicator().sendGroup(nodeGroup, icChunkMessage);
	}
	
	private void calculateAndStoreDigest(int sourceID) {
		MessageDigest datumDigest = digestMap.remove(sourceID);
		if (datumDigest == null)
			throw new RuntimeException("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: The nodeID " + sourceID + " does not exist in the digestMap!");

		byte[] datum = datumDigest.digest();
		finalDigests.put(sourceID, datum);

		if (sourceID == nodeID)
			constBroadcast.broadcast(this.id, -1, nodeID, BroadcastID.CB_BROADCAST_ID.getValue(), GlobalUtils.bytesToHex(datum));
		else if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Stored the final digest " + GlobalUtils.bytesToHex(datum) + " for the node " + sourceID);
	}
	
	public void OnICChunkMessage(ICChunkMessage chunkMessage, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: An ICChunkMessage was received " + chunkMessage.toString() + " from Node " + source.getNodeId());
		
		// Check if the value dissemination phase has been completed.
		if(completed || timeoutFired)
			return;
		
		int sourceID = source.getNodeId();
		int chunkID = chunkMessage.getChunkID();
			
		if (chunkMessage.getChunkData().length == 0) {
			if(chunkID == getNextChunkIDPerNode(sourceID)) {
				// End of datum transmission.
				assert(chunkDataPerNode.get(source.getNodeId()) == null);
				
				setTotalChunksPerNode(sourceID, chunkID);
				
				// Calculate and store the final digest.
				calculateAndStoreDigest(sourceID);
			}
			else {
				// Update the number of total chunks for the specified sourceID.
				setTotalChunksPerNode(sourceID, chunkID);
			}
		}
		else {
			// Check if the incoming chunk is the next in line.
			if (chunkID == getNextChunkIDPerNode(sourceID)) {
				MessageDigest datumDigest;
				if ((datumDigest = digestMap.get(source.getNodeId())) == null) {
					if (finalDigests.containsKey(source.getNodeId()))
						throw new RuntimeException("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Got an ICChunkMessage from a node that has already an entry in the final digests map....");

					try {
						datumDigest = MessageDigest.getInstance(GlobalVariables.DIGEST_ALGORITHM);
					}
					catch (NoSuchAlgorithmException ex) {
						System.err.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Caught a NoSuchAlgorithmException while attempting to get an instance of the SHA-256 message digest algorithm!");
						ex.printStackTrace();
						return;
					}
					digestMap.put(source.getNodeId(), datumDigest);
				}
				datumDigest.update(chunkMessage.getChunkData());
				
				// Update the ID of the next chunk to receive.
				incrNextChunkIDPerNode(sourceID);
				
				// Store the chunk, or fetch the next one.
				assert(storeProcessor != null);
				if (sourceID != nodeID)
					storeProcessor.storeNextChunk(chunkMessage, source);
				else
					getProcessor.getNextChunk(getNextChunkIDPerNode(sourceID));
				
				// Check if there are any more subsequent chunks.
				Map<Integer, byte[]> chunkData = chunkDataPerNode.get(source.getNodeId());
				if(chunkData != null) {
					byte[] data;
					while((data = chunkData.remove(getNextChunkIDPerNode(sourceID))) != null) {
						datumDigest.update(data);
						
						// Store the chunk, or fetch the next one.
						if (source.getNodeId() != nodeID)
							storeProcessor.storeNextChunk(chunkMessage, source);
						else
							getProcessor.getNextChunk(getNextChunkIDPerNode(nodeID) + 1);
						
						// Move on to the next chunk.
						incrNextChunkIDPerNode(sourceID);
					}
					
					// Delete the entry if no more chunks are available for the specified sourceID.
					if(chunkData.size() == 0)
						chunkDataPerNode.remove(source.getNodeId());
				}
				
				// Check if all chunks are processed.
				if(getNextChunkIDPerNode(sourceID) == getTotalChunksPerNode(sourceID)) {
					// Calculate and store the final digest.
					calculateAndStoreDigest(sourceID);
				}
			}
			else {
				Map<Integer, byte[]> chunkData;
				if((chunkData = chunkDataPerNode.get(source.getNodeId())) == null) {
					chunkData = new TreeMap<>();
					chunkDataPerNode.put(source.getNodeId(), chunkData);
				}
				
				chunkData.put(chunkID, chunkMessage.getChunkData());
			}
		}
	}
	
	@Override
	public boolean verifyDatum(String hashAsHexString, int sourceNodeID) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Inside verifyDatum:: currentNodeID = " + nodeID + ", senderNodeID = " + sourceNodeID);
		
		byte[] storedDatum = finalDigests.get(sourceNodeID);
		byte[] receivedDatum = GlobalUtils.hexStringToByteArray(hashAsHexString);
		
		if(storedDatum == null) {
			if(GlobalVariables.HIGH_VERBOSE)
				System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Inside verifyDatum:: " + Arrays.asList(finalDigests).toString());
			
			return false;
		}
		
		if (storedDatum.length != receivedDatum.length) {
			System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Length mismatch in datum comparison!");
			return false;
		}

		for (int i = 0; i < storedDatum.length; ++i) {
			if (storedDatum[i] != receivedDatum[i]) {
				System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Byte i = " + i + " mismatch in datum comparison!");
				return false;
			}
		}
	
		return true;
	}
	
	@Override
	public void processRecoveryRequestMessage(RecoveryRequestMessage recoveryRequestMessage, Node source) {
		if(GlobalVariables.HIGH_VERBOSE) {
			System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: A RecoveryRequestMessage message was received: " + recoveryRequestMessage.toString() + " from Node " + source.getNodeId());
			System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: In processRecoveryRequestMessage():: " + Arrays.asList(nodeValue) + ", " + Arrays.asList(nodeValueSignatures));
		}
		
		// Ignore messages from myself.
		if(source.getNodeId() == nodeID)
			return;
		
		int consensusID = recoveryRequestMessage.getConsensusID();
		String value = nodeValue.get(consensusID);
		if(value != null && !value.equals("null")) {
			AbstractCommunicator communicator = constBroadcast.getCommunicator();
			Map<Integer, byte[]> chunkData = recoveryProcessor.recoverChunks(consensusID);
			if(chunkData == null)
				throw new RuntimeException("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: No chunk data were found during recovery!");
			
			if(GlobalVariables.HIGH_VERBOSE)
				System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: In processRecoveryRequestMessage():: " + Arrays.asList(chunkData));
			
			for(int i = 0; i < getTotalChunksPerNode(consensusID); ++i)
				communicator.send(source, new ICChunkRecoveryMessage(recoveryRequestMessage.getApplicationID(), consensusID, i, chunkData.get(i)));

			ICChunkRecoveryMessage recoveryMsg = new ICChunkRecoveryMessage(recoveryRequestMessage.getApplicationID(), consensusID, getTotalChunksPerNode(consensusID), new byte[0]);
			recoveryMsg.setSignatures(nodeValueSignatures.get(consensusID));
			communicator.send(source, recoveryMsg);
		}
	}

	@Override
	public void processRecoveryResponseMessage(RecoveryResponseMessage recoveryResponseMessage, Node source) {
		throw new RuntimeException("This operation is not supported by a BBInteractiveConsistencyDatum application!");
	}
	
	private int getNextChunkIDRecovery(String UID) {
		Integer nextChunkID = nextChunkIDRecovery.get(UID);
		if(nextChunkID == null) {
			nextChunkID = new Integer(0);
			nextChunkIDRecovery.put(UID, nextChunkID);
		}
		
		return nextChunkID;
	}
	
	private int getTotalChunksRecovery(String UID) {
		Integer totalChunks = totalChunksRecovery.get(UID);
		if(totalChunks == null) {
			totalChunks = new Integer(-1);
			nextChunkIDRecovery.put(UID, totalChunks);
		}
		
		return totalChunks;
	}
	
	public void processICChunkRecoveryMessage(ICChunkRecoveryMessage icChunkRecoveryMessage, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: An ICChunkRecoveryMessage message was received: " + icChunkRecoveryMessage.toString() + " from Node " + source.getNodeId());
				
		// Ignore the message if the corresponding inputValue was received.
		int consensusID = icChunkRecoveryMessage.getConsensusID();
		if(nodeValue.containsKey(consensusID))
			return;
		
		int chunkID = icChunkRecoveryMessage.getChunkID();
		String UID = consensusID + ":" + source.getNodeId();
		
		int nextChunkID = getNextChunkIDRecovery(UID);
		int totalChunks = getTotalChunksRecovery(UID);
		
		if (icChunkRecoveryMessage.getChunkData().length == 0) {
			
			// Store the set of signatures.
			nodeValueSignaturesRecovery.put(UID, icChunkRecoveryMessage.getSignatures());
			
			// Update the number of total chunks for the specified sourceID.
			totalChunksRecovery.put(UID, chunkID);
			
			if(chunkID == nextChunkID) {
				// Calculate and store the final digest.
				calculateAndStoreDigestDatum(icChunkRecoveryMessage, source);
			}
		}
		else {
			// Check if the incoming chunk is the next in line.
			if (chunkID == nextChunkID) {
				MessageDigest datumDigest;
				if ((datumDigest = digestMapRecovery.get(UID)) == null) {
					if (finalDigests.containsKey(consensusID))
						throw new RuntimeException("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Got an ICChunkRecoveryMessage from a node that has already an entry in the final digests map....");

					try {
						datumDigest = MessageDigest.getInstance(GlobalVariables.DIGEST_ALGORITHM);
					}
					catch (NoSuchAlgorithmException ex) {
						System.err.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Caught a NoSuchAlgorithmException while attempting to get an instance of the SHA-256 message digest algorithm!");
						ex.printStackTrace();
						return;
					}
					digestMapRecovery.put(UID, datumDigest);
				}
				datumDigest.update(icChunkRecoveryMessage.getChunkData());
				
				// Update the ID of the next chunk to receive.
				++nextChunkID;
				nextChunkIDRecovery.put(UID, nextChunkID);
				
				// Store the chunk.
				assert(storeProcessor != null);
				if (consensusID != nodeID)
					storeProcessor.storeNextChunk(new ICChunkMessage(icChunkRecoveryMessage.getApplicationID(), chunkID, icChunkRecoveryMessage.getChunkData()), source);
				
				// Check if there are any more subsequent chunks.
				Map<Integer, byte[]> chunkData = chunkDataPerNodeRecovery.get(UID);
				if(chunkData != null) {
					byte[] data;
					while((data = chunkData.remove(nextChunkID)) != null) {
						datumDigest.update(data);
						
						// Store the chunk.
						if (consensusID != nodeID)
							storeProcessor.storeNextChunk(new ICChunkMessage(icChunkRecoveryMessage.getApplicationID(), chunkID, icChunkRecoveryMessage.getChunkData()), source);
						
						// Move on to the next chunk.
						++nextChunkID;
						nextChunkIDRecovery.put(UID, nextChunkID);
					}
					
					// Delete the entry if no more chunks are available for the specified sourceID.
					if(chunkData.size() == 0)
						chunkDataPerNodeRecovery.remove(UID);
				}
				
				// Check if all chunks are processed.
				if(nextChunkID == totalChunks) {
					// Calculate and store the final digest.
					calculateAndStoreDigestDatum(icChunkRecoveryMessage, source);
				}
			}
			else {
				Map<Integer, byte[]> chunkData;
				if((chunkData = chunkDataPerNodeRecovery.get(UID)) == null) {
					chunkData = new TreeMap<>();
					chunkDataPerNodeRecovery.put(UID, chunkData);
				}
				
				chunkData.put(chunkID, icChunkRecoveryMessage.getChunkData());
			}
		}
	}

	private void calculateAndStoreDigestDatum(ICChunkRecoveryMessage icChunkRecoveryMessage, Node source) {
		int consensusID = icChunkRecoveryMessage.getConsensusID();
		String UID = consensusID + ":" + source.getNodeId();
		
		MessageDigest datumDigest = digestMapRecovery.remove(UID);
		if (datumDigest == null)
			throw new RuntimeException("[BBInteractiveConsistencyDatum, Node " + nodeID + "]: The key " + UID + " does not exist in the recovery-digest map!");

		byte[] datum = datumDigest.digest();
		String value = GlobalUtils.bytesToHex(datum);
		
		CryptographyModule cryptoModule = ((ConsistentBroadcast) constBroadcast).getCryptoModule();
		AbstractCommunicator communicator = constBroadcast.getCommunicator();
		String content = icChunkRecoveryMessage.getContent(defaultConsensusID, value);
		Key key = null;
		
		/* If the ICChunkRecoveryMessage does not contain signatures, it means
		 * that the final ICChunkRecoveryMessage for the specified consensusID has already
		 * been received. Thus, we can retrieve the corresponding signatures that were
		 * stored when the final ICChunkRecoveryMessage was processed. */
		if(icChunkRecoveryMessage.getSignatures().size() == 0)
			icChunkRecoveryMessage.setSignatures(nodeValueSignaturesRecovery.get(UID));
		
		for(Integer currentNodeID: icChunkRecoveryMessage.getNodeIDsInSignature()) {
			byte[] signature = null;
			
			if(cryptoModule instanceof DigitalSignatureCryptographyModule) {
				key = communicator.getOtherNode(currentNodeID).getPublicKey();
				signature = icChunkRecoveryMessage.getSignature(currentNodeID);
			}
			else {
				key = communicator.getCurrentNode().getSymmetricKey(currentNodeID);
				signature = icChunkRecoveryMessage.getSignature(currentNodeID, consensusID, cryptoModule.getDigestLength());
			}

			if(!cryptoModule.verify(content, key, signature)) {
				System.err.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: The ICChunkRecoveryMessage: " + icChunkRecoveryMessage.toString()
						+ " received from Node " + source.getNodeId() + " couldn't be verified!");
				return;
			}
		}
		
		// Accept the new value and check if the IC vector is now complete.
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBInteractiveConsistencyDatum, Node: " + nodeID + ", ICID: " + id + "]: Accepting the value " + value + " for consensus ID " + consensusID);
		
		// Store the value and its signature.
		finalDigests.put(consensusID, datum);
		nodeValue.put(consensusID, value);
		nodeValueSignatures.put(consensusID, icChunkRecoveryMessage.getSignatures());
		
		// Clean-up all data structures for the specified Datum recovery UID to save some memory.
		digestMapRecovery.remove(UID);
		chunkDataPerNodeRecovery.remove(UID);
		nodeValueSignaturesRecovery.remove(UID);
		nextChunkIDRecovery.remove(UID);
		totalChunksRecovery.remove(UID);

		if (nodeValue.size() == this.numberOfNodes) {
			/* The specified IC protocol has terminated.
			 * Clear all Recovery data structures. */
			clearRecoveryStructures();
			
			List<String> result = new ArrayList<>();

			for (Integer nodeID : nodeValue.keySet())
				result.add(nodeValue.get(nodeID));

			this.resultProcessor.processResult(this.id, result);
		}
	}

	private void clearRecoveryStructures() {
		nextChunkIDRecovery.clear();
		totalChunksRecovery.clear();
		digestMapRecovery.clear();
		nodeValueSignaturesRecovery.clear();
		chunkDataPerNodeRecovery.clear();
	}
}
