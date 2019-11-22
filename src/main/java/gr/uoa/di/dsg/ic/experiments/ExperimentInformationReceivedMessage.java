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

public class ExperimentInformationReceivedMessage extends Message {
	
	/**
	 * The specific type of this message.
	 */
	public static ExperimentsMessageType messageType = ExperimentsMessageType.EXP_INFORMATION_RECEIVED;
    
	public ExperimentInformationReceivedMessage(int originalSourceNodeId) {
		super("");
                this.originalSourceNodeId = originalSourceNodeId;
	}
	
	public int getType() {
		return messageType.getValue();
	}
	
	public byte[] serialize() {
		return Integer.toString(originalSourceNodeId).getBytes();
	}
	
	public static ExperimentInformationReceivedMessage deserialize(byte[] data) {
		return new ExperimentInformationReceivedMessage(Integer.parseInt(new String(data)));
	}

        @Override
	public String getSubject() {
		return Integer.toString(originalSourceNodeId);
	}

	@Override
	public String toString() {
		return "ExperimentInformationReceivedMessage <" + originalSourceNodeId + ">";
	}
        private int originalSourceNodeId;
}
