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
package gr.uoa.di.dsg.consensus.multivalued.messages;

import gr.uoa.di.dsg.broadcast.BroadcastMessage;
import gr.uoa.di.dsg.communicator.GenericMessageType;

import com.google.protobuf.InvalidProtocolBufferException;

public class MVInitialMessage extends BroadcastMessage {

public static GenericMessageType messageType = MVMessageType.MVINITIAL;
	
	/** The Logger instance for this class. */
	//private final static Logger logger = LoggerFactory.getLogger(MVInitialMessage.class);

	public MVInitialMessage(String interactiveUID, Integer consensusUID, Integer processUID, Integer broadcastUID, String value) {
		super(interactiveUID, consensusUID, processUID, broadcastUID, value);
	}

	@Override
	public String toString() {
		return "MVInitialMessage <" + this.applicationID + ", " + this.consensusID + ", " + this.nodeID + ", " + this.broadcastID + ", " + this.value + ">";
	}

	@Override
	public int getType() {
		return messageType.getValue();
	}

	@Override
	public byte[] serialize() {
		return MultiValueConsensusMessage.MVInit.newBuilder().setBid(getBroadcastID()).setCid(getConsensusID())
				.setPid(getNodeID()).setIcid(getApplicationID())
				.setValue(getValue()).build().toByteArray();
	}

	public static MVInitialMessage deserialize(byte[] rawData) {
		MVInitialMessage mvInitMessage = null;
		
		try {
			MultiValueConsensusMessage.MVInit initMsg = MultiValueConsensusMessage.MVInit.parseFrom(rawData);
			mvInitMessage = new MVInitialMessage(initMsg.getIcid(), initMsg.getCid(), initMsg.getPid(), initMsg.getBid(),initMsg.getValue());
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return mvInitMessage;
	}
	
	public boolean isEqual(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		MVInitialMessage other = (MVInitialMessage) obj;
		
		return applicationID.equals(other.applicationID)
				&& consensusID 	== other.consensusID
				&& nodeID 	== other.nodeID
				&& broadcastID 	== other.broadcastID
				&& value.equals(other.value);
	}

}
