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

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.broadcast.BroadcastMessage;
import gr.uoa.di.dsg.communicator.GenericMessageType;
import gr.uoa.di.dsg.utils.BroadcastID;

import com.google.protobuf.InvalidProtocolBufferException;

public class MVInitMessage extends BroadcastMessage {
	
	public static GenericMessageType messageType = MVMessageType.MVINIT;
	
	/** The Logger instance for this class. */
	//private final static Logger logger = LoggerFactory.getLogger(MVInitMessage.class);

	public MVInitMessage(String interactiveUID, int consensusUID, int processUID, int broadcastUID, String value) {
		super(interactiveUID, consensusUID, processUID, broadcastUID, value);
	}
	
	public MVInitMessage(BroadcastAccept msg) {
		this(msg.getApplicationID(), msg.getConsensusID(), msg.getNodeID(), BroadcastID.MVINIT_BROADCAST_ID.getValue(), msg.getValue());
	}

	@Override
	public String toString() {
		return "MVInitMessage <" + this.applicationID + ", " + this.consensusID + ", " + this.nodeID + ", " + this.broadcastID + ", "
				+ this.value + ">";
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

	public static MVInitMessage deserialize(byte[] rawData) {
		MVInitMessage mvInitMessage = null;
		
		try {
			MultiValueConsensusMessage.MVInit initMsg = MultiValueConsensusMessage.MVInit.parseFrom(rawData);
			mvInitMessage = new MVInitMessage(initMsg.getIcid(), initMsg.getCid(), initMsg.getPid(), initMsg.getBid(),initMsg.getValue());
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
		
		MVInitMessage other = (MVInitMessage) obj;
		
		return applicationID.equals(other.applicationID)
				&& consensusID 	== other.consensusID
				&& nodeID 	== other.nodeID
				&& broadcastID 	== other.broadcastID
				&& value.equals(other.value);
	}
}
