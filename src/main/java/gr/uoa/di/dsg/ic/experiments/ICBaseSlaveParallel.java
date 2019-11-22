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

import java.util.Arrays;

public abstract class ICBaseSlaveParallel extends ICBaseSlave {
	
	public ICBaseSlaveParallel() {
		super();
	}

	@Override
	protected void calculateLastDataPoints() {
		timeValues[K] = Arrays.stream(timeValues, 0, K).max().getAsDouble();
		firstPhaseDuration[K] = Arrays.stream(firstPhaseDuration, 0, K).max().getAsDouble();
	}
	
	@Override
	public void setOutputFilename() {
		outputFilename = new String(outputDirectory + "/" + N + "." + K + "." + nodeId + "." + algorithm + ".parallel.time");
	}
	
	@Override
	public void setFirstPhaseOutputFilename() {
		outputFilename = new String(outputDirectory + "/" + N + "." + K + "." + nodeId + "." + algorithm + ".parallel.firstphase");
	}
}
