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
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcastMessage.CBandRBSSend;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * The Send message being used during the first step of the {@link ConsistentBroadcast}
 * protocol, in order for a node to disseminate its private value.
 * 
 * @author stathis
 *
 */
public class CBSendMessage extends BroadcastMessage {
	
	/**
	 * The specific type of this message.
	 */
	public static BrodacastWithSignaturesMessageType messageType = BrodacastWithSignaturesMessageType.CBSEND;

	/** The Logger instance for this class. */
	//private final static Logger logger = LoggerFactory.getLogger(CBSendMessage.class);

	/**
	 * Constructs a new instance of the {@link CBSendMessage} class.
	 * @param appUID the unique ID of an application
	 * @param consensusUID the ID of the consensus instance, in correspondence to the application ID
	 * @param processUID the original sender of the message
	 * @param broadcastUID the ID of the corresponding broadcast mechanism
	 * @param value the value proposed by the original sender node
	 */
	public CBSendMessage(String appUID, int consensusUID, int processUID, int broadcastUID, String value) {
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
	 * @return the byte array representation of an instance of the {@link CBSendMessage} class.
	 */
	@Override
	public byte[] serialize() {
		return ConsistentBroadcastMessage.CBandRBSSend.newBuilder().setBid(getBroadcastID()).setCid(getConsensusID())
				.setPid(getNodeID()).setIcid(getApplicationID())
				.setValue(getValue()).build().toByteArray();
	}

	/**
	 * Creates a new instance of the {@link CBSendMessage} class, based on the input byte array.
	 * @param data the byte array representation of an instance of the {@link CBSendMessage} class.
	 * @return a new instance of the {@link CBSendMessage} class.
	 */
	public static CBSendMessage deserialize(byte[] data) {
		CBSendMessage cbSendMessage = null;
		
		try {
			CBandRBSSend sendMsg = CBandRBSSend.parseFrom(data);
			cbSendMessage = new CBSendMessage(sendMsg.getIcid(), sendMsg.getCid(), sendMsg.getPid(), sendMsg.getBid(), sendMsg.getValue());
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return cbSendMessage;
	}
	
	@Override
	/**
	 * @return the string representation of an instance of the {@link CBSendMessage} class.
	 */
	public String toString() {
		return "CBSendMessage <" + applicationID + ", " + consensusID + ", " + nodeID + ", " + broadcastID + ", " + value  + ">";
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
		
		CBSendMessage other = (CBSendMessage) obj;
		
		return applicationID.equals(other.applicationID)
				&& consensusID 	== other.consensusID
				&& nodeID 	== other.nodeID
				&& broadcastID 	== other.broadcastID
				&& value.equals(other.value);
	}
}
