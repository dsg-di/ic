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
package gr.uoa.di.dsg.monitoring;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import gr.uoa.di.dsg.utils.GlobalVariables;

public class CPUMonitor implements Monitor {

	private Sigar sigar;
	private volatile boolean running = true;
	private List<String> cpuPercentageInfo;

	public CPUMonitor(Sigar sigar) {
		this.sigar = sigar;
		this.cpuPercentageInfo = new ArrayList<>();
	}

	@Override
    public void terminate() {
        running = false;
    }

	@Override
	public void run() {
		while (running) {
			try {
				Thread.sleep(GlobalVariables.MONITOR_FREQUENCY);

				// Collect different percentages related to the CPU (all cores included).
				// cpuInfo.add(sigar.getCpu().toString());
				cpuPercentageInfo.add(sigar.getCpuPerc().toString());

	            // Collect information for each different core.
	            /* Cpu[] cpuList = sigar.getCpuList();
	            CpuPerc[] cpuPercList = sigar.getCpuPercList();
	            for (int i = 0; i < cpuPercList.length; ++i)
	                printCpu(i + ": ", cpuPercList[i]); */

			} catch (InterruptedException | SigarException ex) {
				// For now, ignore any exceptions.
			}
		}
	}

	private String cpuInfo() throws SigarException {
		CpuInfo cpuInfo = sigar.getCpuInfoList()[0];

		String infoString = cpuInfo.toString();
		infoString += "\nPhysical CPUs: " + cpuInfo.getTotalSockets();
		infoString += "\nCores per CPU: " + cpuInfo.getCoresPerSocket();

		long cacheSize = cpuInfo.getCacheSize();
		if (cacheSize != Sigar.FIELD_NOTIMPL)
			infoString += "\nCache size: " + cacheSize;

		return infoString;
	}

	@Override
	public void print(String prefix) throws IOException {
		try {
			System.out.println(cpuInfo());
		} catch(SigarException ex) {
			// For now, ignore the exception.
		}

		String outputFilename = prefix + ".cpu.data";
		File outputFile = new File(outputFilename);

		/* Create the file, if not exists. */
		if (!outputFile.exists())
			outputFile.createNewFile();

		PrintWriter outputWriter = new PrintWriter(new FileOutputStream(outputFilename, true));

		outputWriter.println("SequenceID,User,System,Nice,Wait,Idle");
		for (int idx = 0; idx < cpuPercentageInfo.size(); ++idx) {
			String cpuPercInfo = cpuPercentageInfo.get(idx);
			String[] tokens = cpuPercInfo.split(":|,");
			assert (tokens.length == 6);

			String line = String.valueOf(idx + 1);
			for (int i = 1; i < tokens.length; ++i) {
				// Make sure that the percentage can be safely converted to a real number.
				double pct = Double.valueOf(tokens[i].substring(0, tokens[i].indexOf("%")));
				line += ("," + pct);
			}
			outputWriter.println(line);
		}
		outputWriter.close();
	}

	@SuppressWarnings("unused")
	private void printCpu(String prefix, CpuPerc cpu) {
        System.out.println(prefix +
                           CpuPerc.format(cpu.getUser()) + "\t" +
                           CpuPerc.format(cpu.getSys()) + "\t" +
                           CpuPerc.format(cpu.getWait()) + "\t" +
                           CpuPerc.format(cpu.getNice()) + "\t" +
                           CpuPerc.format(cpu.getIdle()) + "\t" +
                           CpuPerc.format(cpu.getCombined()));
    }
}
