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

import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.ic.recovery.RecoveryMessages.OwnerAndSignatureTuple;
import gr.uoa.di.dsg.ic.recovery.RecoveryMessages.RecoveryResponse.Builder;
import gr.uoa.di.dsg.utils.BroadcastID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class RecoveryResponseMessage extends Message {

	/**
	 * The specific type of this message.
	 */
	public static RecoveryMessageType messageType = RecoveryMessageType.RESPONSE;
	
	private final int consensusID;
	
	private final String value;
	
	/** The list of signatures accompanying this message. */
	protected Map<Integer, byte[]> signatures = null;
	
	public RecoveryResponseMessage(String appUID, int consensusID, String value) {
		super(appUID);
		this.consensusID = consensusID;
		this.value = value;
		this.signatures = new HashMap<>();
	}
	
	/**
	 * Returns the integer value of the specific message type.
	 * @return the integer value of the specific message type.
	 */
	@Override
	public int getType() {
		return messageType.getValue();
	}
	
	public int getConsensusID() {
		return consensusID;
	}

	public String getValue() {
		return value;
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

	/**
	 * @return the byte array representation of an instance of the {@link CBFinalMessage} class.
	 */
	@Override
	public byte[] serialize() {
		Builder builder = RecoveryMessages.RecoveryResponse.newBuilder().setIcid(applicationID).setCid(consensusID).setValue(getValue());
		
		for(Integer currentNodeID: signatures.keySet())
			builder.addSignature(OwnerAndSignatureTuple.newBuilder().setOwner(currentNodeID).setSignature(ByteString.copyFrom(signatures.get(currentNodeID))).build());
		
		return builder.build().toByteArray();
	}

	/**
	 * Creates a new instance of the {@link CBFinalMessage} class, based on the input byte array.
	 * @param data the byte array representation of an instance of the {@link CBFinalMessage} class.
	 * @return a new instance of the {@link CBFinalMessage} class.
	 */
	public static RecoveryResponseMessage deserialize(byte[] data) {
		RecoveryResponseMessage recoveryResponseMessage = null;
		
		try {
			RecoveryMessages.RecoveryResponse recoveryResponse = RecoveryMessages.RecoveryResponse.parseFrom(data);
			recoveryResponseMessage = new RecoveryResponseMessage(recoveryResponse.getIcid(), recoveryResponse.getCid(), recoveryResponse.getValue());
			
			for(int i = 0; i < recoveryResponse.getSignatureCount(); ++i) {
				OwnerAndSignatureTuple pair = recoveryResponse.getSignature(i);
				recoveryResponseMessage.signatures.put(pair.getOwner(), pair.getSignature().toByteArray());
			}
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return recoveryResponseMessage;
	}
	
	public String getContent(int defaultValue) {
		return applicationID + ":" + defaultValue + ":" + consensusID + ":" + BroadcastID.CB_BROADCAST_ID.getValue() + value; 
	}

	/**
	 * @return the string representation of an instance of the {@link RecoveryResponseMessage} class.
	 */
	@Override
	public String toString() {
		return "RecoveryResponseMessage <" + applicationID + ", " + consensusID + ", " + value + ", " + Arrays.asList(signatures).toString() + ">";
	}
}
