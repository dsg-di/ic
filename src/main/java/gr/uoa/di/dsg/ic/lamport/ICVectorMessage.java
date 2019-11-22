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
import gr.uoa.di.dsg.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;

public class ICVectorMessage extends Message {
	public static GenericMessageType messageType = SynchronousICMessageType.ICVECTOR;
	
	/** The round of the Interactive Consistency protocol. */
	private int round;
	
	/** The process that sends this message. */
	private int processID;
	
	/** The vector of values. */
	private List<String> values;

	public ICVectorMessage(String appID, int round, int processID) {
		super(appID);
		this.values = new ArrayList<>();
		this.round = round;
		this.processID = processID;
	}
	
	public ICVectorMessage(String ICID, int round, int processID, String[] values) {
		super(ICID);
		this.round = round;
		this.processID = processID;
		this.values = StringUtils.arrayToList(values);
	}

	/**
	 * @return the round
	 */
	public int getRound() {
		return round;
	}
	
	/**
	 * @return the processID
	 */
	public int getProcessID() {
		return processID;
	}

	/**
	 * @param round the round to set
	 */
	public void setRound(int round) {
		this.round = round;
	}
	
	/**
	 * @param processID the processID to set
	 */
	public void setProcessID(int processID) {
		this.processID = processID;
	}

	public void addValue(String value) {
		values.add(value);
	}
	
	public String getValue(int index) {
		return values.get(index);
	}
	
	public List<String> getValues() {
		return values;
	}
	
	public void setValues(List<String> values) {
		this.values = values;
	}

	@Override
	public String toString() {
		return "ICVectorMessage <" + applicationID + ", " + round + ", " + processID + ", " + StringUtils.listToString(values) + ">";
	}

	public int getType() {
		return messageType.getValue();
	}

	public byte[] serialize() {
		InteractiveConsistencyMessage.ICVector.Builder builder = InteractiveConsistencyMessage.ICVector.newBuilder()
				.setIcid(applicationID).setRound(round).setPid(processID);
		
		for(int i = 0; i < values.size(); ++i)
			builder.addValues(values.get(i) != null ? values.get(i) : "null");
		
		return builder.build().toByteArray();
	}

	public static ICVectorMessage deserialize(byte[] rawData) {
		ICVectorMessage icVectorMessage = null;
		
		try {
			InteractiveConsistencyMessage.ICVector vectorMsg = InteractiveConsistencyMessage.ICVector.parseFrom(rawData);
			icVectorMessage = new ICVectorMessage(vectorMsg.getIcid(), vectorMsg.getRound(), vectorMsg.getPid());
			
			for(int i = 0; i < vectorMsg.getValuesCount(); ++i)
				icVectorMessage.addValue(vectorMsg.getValues(i));
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return icVectorMessage;
	}
	
	public boolean isEqual(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		ICVectorMessage other = (ICVectorMessage) obj;
		if (values == null) {
			if (other.values != null)
				return false;
		}
		else if (!values.equals(other.values))
			return false;
		
		return applicationID.equals(other.applicationID) && round == other.round && processID == other.processID;
	}
}
