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
import gr.uoa.di.dsg.ic.recovery.RecoveryMessages.RecoveryRequest;

import com.google.protobuf.InvalidProtocolBufferException;

public class RecoveryRequestMessage extends Message {

	/**
	 * The specific type of this message.
	 */
	public static RecoveryMessageType messageType = RecoveryMessageType.REQUEST;
	
	private final int consensusID;
	
	public RecoveryRequestMessage(String appUID, int consensusID) {
		super(appUID);
		this.consensusID = consensusID;
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
	
	/**
	 * @return the byte array representation of an instance of the {@link RecoveryRequestMessage} class.
	 */
	@Override
	public byte[] serialize() {
		return RecoveryRequest.newBuilder().setIcid(applicationID).setCid(consensusID).build().toByteArray();
	}

	/**
	 * Creates a new instance of the {@link RecoveryRequestMessage} class, based on the input byte array.
	 * @param data the byte array representation of an instance of the {@link RecoveryRequestMessage} class.
	 * @return a new instance of the {@link RecoveryRequestMessage} class.
	 */
	public static RecoveryRequestMessage deserialize(byte[] data) {
		RecoveryRequestMessage recoveryRequestMsg = null;
		
		try {
			RecoveryMessages.RecoveryRequest requestMsg = RecoveryMessages.RecoveryRequest.parseFrom(data);
			recoveryRequestMsg = new RecoveryRequestMessage(requestMsg.getIcid(), requestMsg.getCid());
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return recoveryRequestMsg;
	}

	/**
	 * @return the string representation of an instance of the {@link RecoveryRequestMessage} class.
	 */
	@Override
	public String toString() {
		return "RecoveryRequestMessage <" + applicationID + ", " + consensusID + ">";
	}
}
