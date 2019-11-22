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

import gr.uoa.di.dsg.communicator.Message;

/**
 * 
 * @author panos
 * @version 0.0.1
 */
public abstract class BroadcastMessage extends Message
{
	protected int broadcastID;
	protected int nodeID;
	protected int consensusID;
	protected String value;
	
	public BroadcastMessage(String appId, int cid, int pid, int bid, String value) {
		super(appId);
		this.broadcastID = bid;
		this.nodeID = pid;
		this.consensusID = cid;
		this.value = value;
	}

	public String getUUID() {
		return this.applicationID + ":" + this.consensusID + ":" + this.nodeID + ":" + this.broadcastID;
	}

	public void setBroadcastID(int broadcastUID) 
	{
		this.broadcastID = broadcastUID;
	}
	
	public int getBroadcastID() {
		return broadcastID;
	}

	public void setNodeID(int processUID) {
		this.nodeID = processUID;
	}
	
	public int getNodeID() {
		return nodeID;
	}

	public void setConsensusID(int consensusUID) {
		this.consensusID = consensusUID;
	}
	
	public int getConsensusID() {
		return consensusID;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
}
