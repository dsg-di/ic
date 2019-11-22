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
package gr.uoa.di.dsg.ic.recovery;

import gr.uoa.di.dsg.communicator.GenericMessageType;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.ic.bracha.ICDatumMessage.ICChunkRecovery;
import gr.uoa.di.dsg.ic.bracha.ICDatumMessage.ICChunkRecovery.OwnerAndSignatureTuple;
import gr.uoa.di.dsg.ic.bracha.ICMessageType;
import gr.uoa.di.dsg.utils.BroadcastID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class ICChunkRecoveryMessage extends Message {
	
	public static GenericMessageType messageType = ICMessageType.IC_CHUNK_RECOVERY_RESPONSE;

	private final int consensusID;
	private final int chunkID;
	private final byte[] chunkData;
	
	/** The list of signatures accompanying the last ICChunkRecovery message,
	 *  where chunkData.length = 0. In all previous ICChunkRecovery messages,
	 *  the signatures field equals to null. */
	protected Map<Integer, byte[]> signatures = null;
	
	public ICChunkRecoveryMessage(String applicationID, int consensusID, int chunkID, byte[] data) {
		super(applicationID);
		
		this.consensusID = consensusID;
		this.chunkID = chunkID;
		this.chunkData = data;
		this.signatures = new HashMap<>();
	}
	
	public int getConsensusID() {
		return consensusID;
	}

	public int getChunkID() {
		return chunkID;
	}

	public byte[] getChunkData() {
		return chunkData;
	}
	
	@Override
	public int getType() {
		return messageType.getValue();
	}
	
	public Set<Integer> getNodeIDsInSignature() {
		return signatures.keySet();
	}

	public byte[] getSignature(int currentNodeID) {
		return signatures.get(currentNodeID);
	}
	
	public byte[] getSignature(int origNodeID, int selfNodeID, int digestLength) {
		byte[] origSignature = signatures.get(origNodeID);
		
		int offset = 0;
		
		while(offset < origSignature.length) {
			int currentNodeID = (int) origSignature[offset];
			if(currentNodeID == selfNodeID) {
				byte[] nodeSignature = new byte[digestLength];
				System.arraycopy(origSignature, offset + 1, nodeSignature, 0, digestLength);
				
				return nodeSignature;
			}
			
			offset += (digestLength + 1);
		}
		
		throw new RuntimeException("[RecoveryResponseMessage]: The nodeID " + selfNodeID + " was not found in the array of signatures!");
	}
	
	public Map<Integer, byte[]> getSignatures() {
		return signatures;
	}

	public void setSignatures(Map<Integer, byte[]> signatures) {
		this.signatures = signatures;
	}
	
	public String getContent(int defaultValue, String value) {
		return applicationID + ":" + defaultValue + ":" + consensusID + ":" + BroadcastID.CB_BROADCAST_ID.getValue() + value; 
	}

	@Override
	public byte[] serialize() {		
		ICChunkRecovery.Builder builder = ICChunkRecovery.newBuilder().setAppID(applicationID).setCid(consensusID).setChunkId(chunkID).setChunkData(ByteString.copyFrom(chunkData));
		
		for(Integer currentNodeID: signatures.keySet())
			builder.addSignature(OwnerAndSignatureTuple.newBuilder().setOwner(currentNodeID).setSignature(ByteString.copyFrom(signatures.get(currentNodeID))).build());
		
		return builder.build().toByteArray();
	}

	public static ICChunkRecoveryMessage deserialize(byte[] data) {
		ICChunkRecoveryMessage icChunkRecoveryMsg = null;
		
		try {
			ICChunkRecovery icChunkRecovery = ICChunkRecovery.parseFrom(data);
			icChunkRecoveryMsg = new ICChunkRecoveryMessage(icChunkRecovery.getAppID(), icChunkRecovery.getCid(), icChunkRecovery.getChunkId(), icChunkRecovery.getChunkData().toByteArray());
			
			for(int i = 0; i < icChunkRecovery.getSignatureCount(); ++i) {
				OwnerAndSignatureTuple pair = icChunkRecovery.getSignature(i);
				icChunkRecoveryMsg.signatures.put(pair.getOwner(), pair.getSignature().toByteArray());
			}
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return icChunkRecoveryMsg;
	}
	
	@Override
	public String toString() {
		return "ICChunkRecoveryMessage <" + applicationID + ", " + consensusID + ", " + chunkID + ", " + chunkData.length + ", " + Arrays.asList(signatures) + ">";
	}
}
