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

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.ic.ICResult;
import gr.uoa.di.dsg.ic.bracha.ICChunkMessage;
import gr.uoa.di.dsg.utils.BroadcastID;

public class SynchronousIC implements Application {
	
	/** The unique ID of an Interactive Consistency instance. */
	private final String ICID;
	
	/** The broadcast module of this application. */
	private final IBroadcast broadcastModule;
	
	/** The module that processes the IC results. */
	private ICResult resultProcessor = null;
	
	public SynchronousIC(String ICID, IBroadcast broadcastModule, ICResult resultProcessor) {
		this.ICID = ICID;
		this.broadcastModule = broadcastModule;
		this.resultProcessor = resultProcessor;
	}
	
	@Override
	public void start(int nodeID, String value) {
		broadcastModule.broadcast(ICID, 1, nodeID, BroadcastID.IC_BROADCAST_ID.getValue(), value);
	}
	
	@Override
	public void start() {
		throw new RuntimeException("This operation is not supported by a SynchronousIC application!");
	}
	
	@Override
	public void processDatumChunk(ICChunkMessage icChunkMessage) {
		throw new RuntimeException("This operation is not supported by a SynchronousIC application!");
	}
	
	@Override
	public boolean verifyDatum(String value, int sourceNodeID) {
		throw new RuntimeException("This operation is not supported by a SynchronousIC application!");
	}

	@Override
	public void process(Message msg) {
		if(!msg.getClass().equals(ICDeliverMessage.class))
			throw new RuntimeException("A SynchronousIC application cannot support messages of class " + msg.getClass());

		resultProcessor.processResult(msg.getApplicationID(), ((ICDeliverMessage) msg).getValuesAsList());
	}

	@Override
	public void processConsensusResult(int cid, String value) {
		throw new RuntimeException("This operation is not supported by a SynchronousIC application!");
	}

	@Override
	public IBroadcast getBroadcast() {
		return broadcastModule;
	}
}
