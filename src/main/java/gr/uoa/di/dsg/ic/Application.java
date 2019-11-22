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
package gr.uoa.di.dsg.ic;

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.ic.bracha.ICChunkMessage;

public interface Application {
	public void process(Message msg);
	public void processConsensusResult(int cid, String value);
	public IBroadcast getBroadcast();
	public void start(int nodeID, String value);
	
	/** Methods for the datum feature. */
	public void start();
	public void processDatumChunk(ICChunkMessage icChunkMessage);
	public boolean verifyDatum(String hashAsHexString, int sourceNodeID);
}
