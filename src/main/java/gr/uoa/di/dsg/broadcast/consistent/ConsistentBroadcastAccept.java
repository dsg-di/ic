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
import java.util.Map;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;

public class ConsistentBroadcastAccept extends BroadcastAccept {
	
	/** The list of signatures accompanying this message. */
	private final Map<Integer, byte[]> signatures;

	public ConsistentBroadcastAccept(String icid, int cid, int pid, int bid, String value, Map<Integer, byte[]> signatures) {
		super(icid, cid, pid, bid, value);
		this.signatures = signatures;
	}
	
	public Map<Integer, byte[]> getSignatures() {
		return signatures;
	}

	@Override
	public byte[] serialize() {
		throw new UnsupportedOperationException("Serialize operation is not applicable for BroadcastAccept class");
	}

	@Override
	public String toString() {
		return "ConsistentBroadcastAccept <" + applicationID + ", " + consensusID + ", " + nodeID + ", " + broadcastID + ", " + value + ", " + Arrays.asList(signatures) + ">";
	}
}
