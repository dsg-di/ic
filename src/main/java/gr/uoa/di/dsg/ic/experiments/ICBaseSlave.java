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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import gr.uoa.di.dsg.communicator.AbstractCommunicator;

public abstract class ICBaseSlave {

	protected int N;
	protected int K;
	protected int warmupRounds;
	protected int nodeId;
	protected String algorithm;
	protected String executionMode;
	protected String outputDirectory;
	protected String outputFilename = null;

	protected double[] timeValues = null;
	protected double[] firstPhaseDuration = null;

	protected int operationsInEachExecution;
	protected int totalExecutedOperations = 0;
	protected long rendezvousTime;
	
	protected boolean inWarmupPhase = true;
	
	protected AbstractCommunicator comm = null;
	
	public abstract void executeTest();
	protected abstract void calculateLastDataPoints();
	protected abstract void setOutputFilename();
	protected abstract void setFirstPhaseOutputFilename();

	// Call this function at the beginning of this iteration
	public void markStartTime(int kapa) {
		if (timeValues[kapa - 1] != -1)
			throw new RuntimeException("Attempting to overwrite a data point for k = " + kapa + " that already has it's value set!");
		
		timeValues[kapa - 1] = System.currentTimeMillis();
	}

	/**
	 * Call this function at the end of each iteration, passing as argument
	 * the index of the iteration (starting from 1, not 0).
	 * @param kapa, the index associated with the current running instance.
	 */
	public void markDataPoint(int kapa) {
		double endTime = System.currentTimeMillis();
		
		if (kapa < 1 || kapa > K)
			throw new RuntimeException("Invalid index specified for data point. Value k = " + kapa + " specified is out of range!");
		
		timeValues[kapa - 1] = endTime - timeValues[kapa - 1];
		if (timeValues[kapa - 1] < 0)
			throw new RuntimeException("Calculated a data point time for k = " + kapa + " that has a negative value of " + timeValues[kapa - 1]);
	}
	
	public void markFirstPhaseEndTime(int kapa) {
		double firstPhaseEndTime = System.currentTimeMillis();
		
		if (kapa < 1 || kapa > K)
			throw new RuntimeException("Invalid index specified for data point. Value k = " + kapa + " specified is out of range!");
		
		if (timeValues[kapa - 1] < 0)
			throw new RuntimeException("Calculated a data point time for k = " + kapa + " that has a negative time-value: " + timeValues[kapa - 1]);
		
		if (firstPhaseDuration[kapa - 1] != 0)
			throw new RuntimeException("Duration of the first phase for k = " + kapa + " has already been set!");
		
		firstPhaseDuration[kapa - 1] = firstPhaseEndTime - timeValues[kapa - 1];
		if (firstPhaseDuration[kapa - 1] < 0)
			throw new RuntimeException("Calculated a data point time for k = " + kapa + " that has a negative first phase duration: " + firstPhaseDuration[kapa - 1]);
	}
	
	private void appendTimeToFile(double[] valuesArray) {
		for (int i = 0; i < valuesArray.length; ++i)
			if (valuesArray[i] == -1)
				throw new RuntimeException("Time value is not set properly for repetition " + (i + 1));

		File outputFile = new File(outputFilename);
		File outputDir = new File(outputDirectory);
		try {
			/* Create all necessary directories. */
			if (!outputDir.exists())
				outputDir.mkdirs();

			/* Create the file, if not exists. */
			if (!outputFile.exists())
				outputFile.createNewFile();

			PrintWriter outputWriter = new PrintWriter(new FileOutputStream(outputFilename, true));
			for (int i = 0; i < valuesArray.length - 1; ++i)
				outputWriter.print(valuesArray[i] + " ");
			outputWriter.println(valuesArray[K]);
			
			outputWriter.close();
		}
		catch (IOException ex) {
			System.err.println("An IOException was caught: " + ex.getMessage());
			ex.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void appendTimeToFile() {
		calculateLastDataPoints();
		
		/* Create the name of the output file. */
		this.setOutputFilename();
		appendTimeToFile(timeValues);

		/* Create the name of the output file regarding the completion time of the first phase. */
		this.setFirstPhaseOutputFilename();
		appendTimeToFile(firstPhaseDuration);
	}
}
