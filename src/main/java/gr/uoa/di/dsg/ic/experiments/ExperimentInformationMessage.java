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

public class ExperimentInformationMessage extends Message {
	
	/**
	 * The specific type of this message.
	 */
	public static ExperimentsMessageType messageType = ExperimentsMessageType.EXP_INFORMATION;

    private final int warmupRounds;
    private final String outputDirectory;
    
	public ExperimentInformationMessage(int warmupRounds, String outputDirectory) {
		super("");
		
		this.warmupRounds = warmupRounds;
		this.outputDirectory = outputDirectory;
	}

	@Override
	public String toString() {
		return ("ExperimentInformationMessage <" + warmupRounds + ", " + outputDirectory + ">");
	}

	public int getWarmupRounds() {
		return warmupRounds;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}
	
	public int getType() {
		return messageType.getValue();
	}
	
	public byte[] serialize() {
		return ExperimentMessages.ExperimentInformation.newBuilder()
				.setWarmupRounds(warmupRounds)
				.setOutputDirectory(outputDirectory)
				.build().toByteArray();
	}
	
	public static ExperimentInformationMessage deserialize(byte[] data) {
		ExperimentInformationMessage experimentInformationMessage = null;
		
		try {
			ExperimentMessages.ExperimentInformation experimentInformation = ExperimentMessages.ExperimentInformation.parseFrom(data);
			experimentInformationMessage = new ExperimentInformationMessage(experimentInformation.getWarmupRounds(), experimentInformation.getOutputDirectory());
		}
		catch (InvalidProtocolBufferException ex) {
			System.err.println("An InvalidProtocolBufferException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return experimentInformationMessage;
	}
}
