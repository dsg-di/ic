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
package gr.uoa.di.dsg.broadcast.bracha;

import gr.uoa.di.dsg.broadcast.BroadcastMessage;

import com.google.protobuf.InvalidProtocolBufferException;

public class ReadyMessage extends BroadcastMessage{

	public static BBMessageType myType = BBMessageType.READY;
	
	public ReadyMessage(String icid, int cid, int pid, int bid, String value) {
		super(icid, cid, pid, bid, value);
	}
	
	@Override
	public byte[] serialize() {
		return BrachaBroadcastMessage.ReadyMessage.newBuilder()
									 .setBid(getBroadcastID())
									 .setCid(getConsensusID())
									 .setPid(getNodeID())
									 .setIcid(getApplicationID())
									 .setValue(getValue()).build().toByteArray();
	}
	
	@Override
	public int getType() {
		return myType.getValue();
	}
	
	public static ReadyMessage deserialize(byte[] rawData) {
			
		ReadyMessage msg = null;
		try {
			BrachaBroadcastMessage.ReadyMessage dummy = BrachaBroadcastMessage.ReadyMessage.parseFrom(rawData);
			msg = new ReadyMessage(dummy.getIcid(),dummy.getCid(),dummy.getPid(),dummy.getBid(), dummy.getValue());
			
		} catch (InvalidProtocolBufferException e) {
			msg = null;
			e.printStackTrace();
		}
		return msg;
	}
	
	@Override
	public String toString() {
		return "ReadyMessage <" + applicationID + ", " + consensusID + ", " + nodeID + ", " + broadcastID + ", " + value + ">";
	}
}
