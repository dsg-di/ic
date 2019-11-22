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

import java.util.Arrays;

import com.google.protobuf.InvalidProtocolBufferException;

public class RBSEchoMessage extends CBEchoMessage {

	public static BrodacastWithSignaturesMessageType messageType = BrodacastWithSignaturesMessageType.RBSECHO;
	
	public RBSEchoMessage(String interactiveUID, int consensusUID, int processUID, int broadcastUID, String value) {
		super(interactiveUID, consensusUID, processUID, broadcastUID, value);
	}
	
	@Override
	public int getType() {
		return messageType.getValue();
	}

	public static RBSEchoMessage deserialize(byte[] data) {
		RBSEchoMessage rbsEchoMessage = null;
		
		try {
			ConsistentBroadcastMessage.CBandRBSEcho cbAndRBSEchoMsg = ConsistentBroadcastMessage.CBandRBSEcho.parseFrom(data);
			rbsEchoMessage = new RBSEchoMessage(cbAndRBSEchoMsg.getIcid(), cbAndRBSEchoMsg.getCid(), cbAndRBSEchoMsg.getPid(), cbAndRBSEchoMsg.getBid(), cbAndRBSEchoMsg.getValue());
			rbsEchoMessage.signature = cbAndRBSEchoMsg.getSignature().toByteArray();
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return rbsEchoMessage;
	}

	@Override
	public String toString() {
		return "RBSEchoMessage <" + applicationID + ", " + consensusID + ", " + nodeID + ", " + broadcastID + ", " + value + ", "
				+ Arrays.asList(signature).toString() + ">";
	}
}
