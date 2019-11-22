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
package gr.uoa.di.dsg.ic.bracha;

import gr.uoa.di.dsg.communicator.GenericMessageType;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.ic.bracha.ICDatumMessage.ICChunk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class ICChunkMessage extends Message {
	
	public static GenericMessageType messageType = ICMessageType.IC_CHUNK;

	private final int chunkID;
	private final byte[] chunkData;
	
	public ICChunkMessage(String applicationID, int chunkID, byte[] data) {
		super(applicationID);
		
		this.chunkID = chunkID;
		this.chunkData = data;
	}

	public int getChunkID() {
		return chunkID;
	}

	public byte[] getChunkData() {
		return chunkData;
	}

	@Override
	public int getType() {
		return messageType.getValue();
	}

	@Override
	public byte[] serialize() {		
		return ICChunk.newBuilder().setChunkId(chunkID).setAppID(applicationID).setChunkData(ByteString.copyFrom(chunkData)).build().toByteArray();
	}

	public static ICChunkMessage deserialize(byte[] data) {
		ICChunkMessage msg = null;
		
		try {
			ICChunk chunk = ICChunk.parseFrom(data);
			msg = new ICChunkMessage(chunk.getAppID(), chunk.getChunkId(), chunk.getChunkData().toByteArray());
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return msg;
	}
	
	@Override
	public String toString() {
		return "ICChunkMessage <" + applicationID + ", " + chunkID + ", " + chunkData.length + ">";
	}
}
