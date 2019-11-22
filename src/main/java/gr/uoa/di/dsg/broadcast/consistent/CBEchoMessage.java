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
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcastMessage.CBandRBSEcho;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * The Echo message used by the {@link ConsistentBroadcast} protocol, as a
 * reply to an incoming {@link CBSendMessage}.
 * 
 * A {@link CBEchoMessage}, along with the proposed value, must also contain the
 * signature of the sender, in order for the message to be successfully verified
 * by the recipient node. Otherwise, the message will be discarded by the recipient
 * node.
 * 
 * The signature can be either a digital signature, or an authenticator.
 * 
 * @author stathis
 *
 */
public class CBEchoMessage extends BroadcastMessage {

	/**
	 * The specific type of this message.
	 */
	public static BrodacastWithSignaturesMessageType messageType = BrodacastWithSignaturesMessageType.CBECHO;
	
	/** The signature accompanying this message. */
	protected byte[] signature = null;
	
	/**
	 * Constructs a new instance with any empty signature. The signature must be
	 * set using the {@link #addSignature(String, byte[])} method.
	 * @param appUID the unique ID of an application
	 * @param consensusUID the ID of the consensus instance, in correspondence to the application ID
	 * @param processUID the original sender of the message
	 * @param broadcastUID the ID of the corresponding broadcast mechanism
	 * @param value the value proposed by the original sender node
	 */
	public CBEchoMessage(String appUID, int consensusUID, int processUID, int broadcastUID, String value) {
		super(appUID, consensusUID, processUID, broadcastUID, value);
	}
	
	
	/**
	 * Returns the integer value of the specific message type.
	 * @return the integer value of the specific message type.
	 */
	@Override
	public int getType() {
		return messageType.getValue();
	}
	
	/**
	 * Returns the digital signature (or authenticator) associated with this message.
	 * @return the digital signature (or authenticator) associated with this message.
	 */
	public byte[] getSignature() {
		return signature;
	}
	
	public byte[] getSignature(int selfNodeID, int digestLength) {
		int offset = 0;
		
		while(offset < signature.length) {
			int currentNodeID = (int) signature[offset];
			if(currentNodeID == selfNodeID) {
				byte[] nodeSignature = new byte[digestLength];
				System.arraycopy(signature, offset + 1, nodeSignature, 0, digestLength);
				
				return nodeSignature;
			}
			
			offset += (digestLength + 1);
		}
		
		throw new RuntimeException("[CBEchoMessasge]: The nodeID " + selfNodeID + " was not found in the array of signatures!");
	}
	
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
	
	public void setSignature(List<Integer> nodeIDs, List<byte[]> signatures) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		for(int i = 0; i < nodeIDs.size(); ++i) {
			int currentNodeID = nodeIDs.get(i);
			outputStream.write((byte) currentNodeID);
			outputStream.write(signatures.get(i));
		}
		
		signature = outputStream.toByteArray();
	}

	/**
	 * @return the byte array representation of an instance of the {@link CBEchoMessage} class.
	 */
	@Override
	public byte[] serialize() {
		return CBandRBSEcho.newBuilder().setBid(getBroadcastID()).setCid(getConsensusID()).setPid(getNodeID()).setIcid(getApplicationID())
				.setValue(getValue()).setSignature(ByteString.copyFrom(signature)).build().toByteArray();
	}

	/**
	 * Creates a new instance of the {@link CBEchoMessage} class, based on the input byte array.
	 * @param data the byte array representation of an instance of the {@link CBEchoMessage} class.
	 * @return a new instance of the {@link CBEchoMessage} class.
	 */
	public static CBEchoMessage deserialize(byte[] data) {
		CBEchoMessage cbEchoMessage = null;
		
		try {
			ConsistentBroadcastMessage.CBandRBSEcho echoMsg = ConsistentBroadcastMessage.CBandRBSEcho.parseFrom(data);
			cbEchoMessage = new CBEchoMessage(echoMsg.getIcid(), echoMsg.getCid(), echoMsg.getPid(), echoMsg.getBid(), echoMsg.getValue());
			cbEchoMessage.signature = echoMsg.getSignature().toByteArray();
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return cbEchoMessage;
	}

	/**
	 * @return the string representation of an instance of the {@link CBEchoMessage} class.
	 */
	@Override
	public String toString() {
		return "CBEchoMessage <" + applicationID + ", " + consensusID + ", " + nodeID + ", " + broadcastID + ", " + value + ", "
				+ Arrays.asList(signature).toString() + ">";
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
		
		CBEchoMessage other = (CBEchoMessage) obj;
		if (signature == null) {
			if (other.signature != null)
				return false;
		}
		else {
			if(other.signature == null)
				return false;
				
			if(signature.length != other.signature.length)
				return false;
				
			for(int j = 0; j < signature.length; ++j)
				if(signature[j] != other.signature[j])
					return false;
		}
		
		return applicationID.equals(other.applicationID)
				&& consensusID 	== other.consensusID
				&& nodeID 	== other.nodeID
				&& broadcastID 	== other.broadcastID
				&& value.equals(other.value);
	}
}
