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
package gr.uoa.di.dsg.ic.lamport;

import gr.uoa.di.dsg.communicator.GenericMessageType;
import gr.uoa.di.dsg.communicator.Message;
import com.google.protobuf.InvalidProtocolBufferException;

public class ICInitMessage extends Message {

	public static GenericMessageType messageType = SynchronousICMessageType.ICINIT;
	
	/** The round of the Interactive Consistency protocol. */
	private int round;
	
	/** The process that sends this message. */
	private int processID;
	
	/** The value of this message. */
	private String value;
	
	public ICInitMessage(String appID, int round, int processID, String value) {
		super(appID);
		this.round = round;
		this.processID = processID;
		this.value = value;
	}
	
	public int getRound() {
		return round;
	}
	
	public int getProcessID() {
		return processID;
	}

	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return "ICInitMessage: <" + this.applicationID + ", " + this.round + ", " + this.processID + ", " + this.value + ">";
	}

	@Override
	public int getType() {
		return messageType.getValue();
	}

	@Override
	public byte[] serialize() {
		return InteractiveConsistencyMessage.ICInit.newBuilder().setIcid(applicationID).setRound(round).setPid(processID).setValue(value).build().toByteArray();
	}

	public static ICInitMessage deserialize(byte[] rawData) {
		ICInitMessage icInitMessage = null;
		
		try {
			InteractiveConsistencyMessage.ICInit initMsg = InteractiveConsistencyMessage.ICInit.parseFrom(rawData);
			icInitMessage = new ICInitMessage(initMsg.getIcid(), initMsg.getRound(), initMsg.getPid(), initMsg.getValue());
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return icInitMessage;
	}
	
	public boolean isEqual(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		ICInitMessage other = (ICInitMessage) obj;
		
		if (value == null) {
			if (other.value != null)
				return false;
		}
		else if (!value.equals(other.value))
			return false;
		
		return applicationID.equals(other.applicationID) && round == other.round && processID == other.processID;
	}
}
