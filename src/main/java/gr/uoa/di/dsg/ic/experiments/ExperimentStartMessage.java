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
package gr.uoa.di.dsg.ic.experiments;

import gr.uoa.di.dsg.communicator.Message;

import com.google.protobuf.InvalidProtocolBufferException;

public class ExperimentStartMessage extends Message {
    
	/**
	 * The specific type of this message.
	 */
	public static ExperimentsMessageType messageType = ExperimentsMessageType.EXP_START;
	
	private final boolean inWarmupPhase;
    private final int operationsInEachExecution;
    private final long rendezvousTime;
    
	public ExperimentStartMessage(boolean inWarmupPhase, int operationsInEachExecution, long rendezvousTime) {
		super("");
		
		this.inWarmupPhase = inWarmupPhase;
		this.operationsInEachExecution = operationsInEachExecution;
		this.rendezvousTime = rendezvousTime;
	}

	@Override
	public String toString() {
		return ("StartMessage <" + operationsInEachExecution + ", " + rendezvousTime + ">");
	}
	
	public int getOperationsInEachExecution() {
		return operationsInEachExecution;
	}
	
	public long getRendezvousTime() {
		return rendezvousTime;
	}
	
	public boolean inWarmupPhase() {
		return inWarmupPhase;
	}
	
	public int getType() {
		return messageType.getValue();
	}
	
	public byte[] serialize() {
		return ExperimentMessages.ExperimentStart.newBuilder().setInWarmupPhase(inWarmupPhase).setOperationsInEachExecution(operationsInEachExecution).setRendezvousTime(rendezvousTime).build().toByteArray();
	}
	
	public static ExperimentStartMessage deserialize(byte[] data) {
		ExperimentStartMessage startMessage = null;
		
		try {
			ExperimentMessages.ExperimentStart startMsg = ExperimentMessages.ExperimentStart.parseFrom(data);
			startMessage = new ExperimentStartMessage(startMsg.getInWarmupPhase(), startMsg.getOperationsInEachExecution(), startMsg.getRendezvousTime());
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return startMessage;
	}
}
