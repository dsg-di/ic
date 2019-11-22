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
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcastMessage.CBandRBSFinal;
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcastMessage.CBandRBSFinal.Builder;
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcastMessage.CBandRBSFinal.OwnerAndSignatureTuple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * The Final message used by the {@link ConsistentBroadcast} protocol during its
 * third step, once the proper number of {@link CBEchoMessage} has been collected
 * during the second step of the protocol.
 * 
 * A {@link CBFinalMessage}, along with the proposed value, must also contain the
 * signatures of all senders, in order for the message to be successfully verified
 * by the recipient node. Otherwise, the message will be discarded by the recipient
 * node.
 * 
 * The signatures can be either digital signatures, or authenticators.
 * 
 * @author stathis
 *
 */
public class CBFinalMessage extends BroadcastMessage {
	
	/**
	 * The specific type of this message.
	 */
	public static BrodacastWithSignaturesMessageType messageType = BrodacastWithSignaturesMessageType.CBFINAL;

	/** The list of signatures accompanying this message. */
	protected Map<Integer, byte[]> signatures = null;
	
	/**
	 * Constructs a new instance with any empty signature list. The signature list must
	 * be set using the {@link #addSignature(String, Map)} method.
	 * @param appUID the unique ID of an application
	 * @param consensusUID the ID of the consensus instance, in correspondence to the application ID
	 * @param processUID the original sender of the message
	 * @param broadcastUID the ID of the corresponding broadcast mechanism
	 * @param value the value proposed by the original sender node
	 */
	public CBFinalMessage(String appUID, int consensusUID, int processUID, int broadcastUID, String value) {
		super(appUID, consensusUID, processUID, broadcastUID, value);
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
	
	public void addSignature(int currentNodeID, byte[] signature) {
		signatures.put(currentNodeID, signature);
	}
	
	public Map<Integer, byte[]> getSignatures() {
		return signatures;
	}

	public int getTotalSignatures() {
		return signatures.size();
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
		
		throw new RuntimeException("[CBFinalMessasge]: The nodeID " + nodeID + " was not found in the array of signatures!");
	}

	/**
	 * @return the byte array representation of an instance of the {@link CBFinalMessage} class.
	 */
	@Override
	public byte[] serialize() {
		Builder builder = CBandRBSFinal.newBuilder().setBid(getBroadcastID()).setCid(getConsensusID()).setPid(getNodeID())
				.setIcid(getApplicationID()).setValue(getValue());
		
		for(Integer currentNodeID: signatures.keySet())
			builder.addSignature(OwnerAndSignatureTuple.newBuilder().setOwner(currentNodeID).setSignature(ByteString.copyFrom(signatures.get(currentNodeID))).build());
		
		return builder.build().toByteArray();
	}

	/**
	 * Creates a new instance of the {@link CBFinalMessage} class, based on the input byte array.
	 * @param data the byte array representation of an instance of the {@link CBFinalMessage} class.
	 * @return a new instance of the {@link CBFinalMessage} class.
	 */
	public static CBFinalMessage deserialize(byte[] data) {
		CBFinalMessage cbFinalMessage = null;
		
		try {
			ConsistentBroadcastMessage.CBandRBSFinal finalMsg = ConsistentBroadcastMessage.CBandRBSFinal.parseFrom(data);
			cbFinalMessage = new CBFinalMessage(finalMsg.getIcid(), finalMsg.getCid(), finalMsg.getPid(), finalMsg.getBid(), finalMsg.getValue());
			
			for(int i = 0; i < finalMsg.getSignatureCount(); ++i) {
				OwnerAndSignatureTuple pair = finalMsg.getSignature(i);
				cbFinalMessage.signatures.put(pair.getOwner(), pair.getSignature().toByteArray());
			}
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return cbFinalMessage;
	}
	
	/**
	 * @return the string representation of an instance of the {@link CBFinalMessage} class.
	 */
	@Override
	public String toString() {
		return "CBFinalMessage <" + applicationID + ", " + consensusID + ", " + nodeID + ", " + broadcastID + ", " + value + ", " + Arrays.asList(signatures).toString();
	}
	
	/**
	 * Indicates whether the specified objects are equal.
	 * @param obj the object to be tested for equality.
	 * @return true if the objects are equal, of false otherwise.
	 */
	public boolean isEqual(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		CBFinalMessage other = (CBFinalMessage) obj;
		if (signatures == null) {
			if (other.signatures != null)
				return false;
		}
		else {
			if(signatures.size() != other.signatures.size())
				return false;
			
			for(Integer key: signatures.keySet()) {
				byte[] signature = signatures.get(key);
				byte[] otherSignature = other.signatures.get(key);
				
				if(signature == null) {
					if(otherSignature != null)
						return false;
				}
				else {
					if(otherSignature == null)
						return false;
					
					if(signature.length != otherSignature.length)
						return false;
					
					for(int j = 0; j < signature.length; ++j)
						if(signature[j] != otherSignature[j])
							return false;
				}
			}
		}
		
		return applicationID.equals(other.applicationID)
				&& consensusID 	== other.consensusID
				&& nodeID 	== other.nodeID
				&& broadcastID 	== other.broadcastID
				&& value.equals(other.value);
	}
}
