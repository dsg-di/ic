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
package gr.uoa.di.dsg.broadcast;

import gr.uoa.di.dsg.broadcast.bracha.BBMessageType;

public class BroadcastAccept extends BroadcastMessage {

	public BroadcastAccept(String icid, int cid, int pid, int bid, String value) {
		super(icid, cid, pid, bid, value);
	}

	public static BBMessageType myType = BBMessageType.ACCEPT;
	
	@Override
	public int getType() {
		return myType.getValue();
	}

	@Override
	public byte[] serialize() {
		throw new UnsupportedOperationException("Serialize operation is not applicable for BroadcastAccept class");
	}

	@Override
	public String toString() {
		return "BroadcastAccept <" + applicationID + ", " + consensusID + ", " + nodeID + ", " + broadcastID + ", " + value + ">";
	}
}
