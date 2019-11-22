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
import gr.uoa.di.dsg.utils.StringUtils;

import java.util.Arrays;

import com.google.protobuf.InvalidProtocolBufferException;

public class MVVectorMessage extends BroadcastMessage {
	
	public static GenericMessageType messageType = MVMessageType.MVVECTOR;

	/** The Logger instance for this class. */
	//private final static Logger logger = LoggerFactory.getLogger(MVVectorMessage.class);
	
	/** The vector of values that accompany the "weighted" value. */
	private String[] vectorOfValues = null;
	
	public MVVectorMessage(String interactiveUID, int consensusUID, int processUID, int broadcastUID, String value) {
		super(interactiveUID, consensusUID, processUID, broadcastUID, value);
		this.vectorOfValues = null;
	}

	public MVVectorMessage(String interactiveUID, int consensusUID, int processUID, int broadcastUID, String value, String[] values) {
		super(interactiveUID, consensusUID, processUID, broadcastUID, value);

		this.vectorOfValues = new String[values.length];
		for (int i = 0; i < values.length; ++i)
			this.vectorOfValues[i] = values[i];
	}
	
	public MVVectorMessage(BroadcastAccept msg) {
		this(msg.getApplicationID(), msg.getConsensusID(), msg.getNodeID(), BroadcastID.MVVECTOR_BROADCAST_ID.getValue(), msg.getValue());
		this.vectorOfValues = null;
	}
	
	public void setVectorOfValues(String[] values) {
		this.vectorOfValues = values;
	}

	public String[] getVectorOfValues() {
		return vectorOfValues;
	}

	@Override
	public String toString() {
		return "MVVectorMessage <" + this.applicationID + ", " + this.consensusID + ", " + this.nodeID + ", " + this.broadcastID + ", "
				+ this.value + ", <" + StringUtils.arrayToString(vectorOfValues) + ">>";
	}

	@Override
	public int getType() {
		return messageType.getValue();
	}

	@Override
	public byte[] serialize() {
		MultiValueConsensusMessage.MVVector.Builder builder = MultiValueConsensusMessage.MVVector
				.newBuilder().setBid(getBroadcastID())
				.setCid(getConsensusID()).setPid(getNodeID())
				.setIcid(getApplicationID()).setValue(getValue());
		
		for(int i = 0; i < vectorOfValues.length; ++i)
			builder.addInitValues(vectorOfValues[i]);
		
		return builder.build().toByteArray();
	}

	public static MVVectorMessage deserialize(byte[] rawData) {
		MVVectorMessage mvVectorMessage = null;
		
		try {
			MultiValueConsensusMessage.MVVector vectorMsg = MultiValueConsensusMessage.MVVector.parseFrom(rawData);
			mvVectorMessage = new MVVectorMessage(vectorMsg.getIcid(), vectorMsg.getCid(), vectorMsg.getPid(), vectorMsg.getBid(), vectorMsg.getValue());
			
			mvVectorMessage.vectorOfValues = new String[vectorMsg.getInitValuesCount()];
			for(int i = 0; i < vectorMsg.getInitValuesCount(); ++i)
				mvVectorMessage.vectorOfValues[i] = vectorMsg.getInitValues(i);
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return mvVectorMessage;
	}

	public boolean isEqual(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		MVVectorMessage other = (MVVectorMessage) obj;
		if (!Arrays.equals(vectorOfValues, other.vectorOfValues))
			return false;
		
		return applicationID.equals(other.applicationID)
				&& consensusID 	== other.consensusID
				&& nodeID 	== other.nodeID
				&& broadcastID 	== other.broadcastID
				&& value.equals(other.value);
	}
}
