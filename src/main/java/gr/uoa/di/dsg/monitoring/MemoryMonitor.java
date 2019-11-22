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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import gr.uoa.di.dsg.utils.GlobalVariables;

public class MemoryMonitor implements Monitor {

	private Sigar sigar;
	private String pid;
	private volatile boolean running = true;
	private List<Long> procMemInfoList;

	public MemoryMonitor(Sigar sigar) {
		this.sigar = sigar;
		this.pid = String.valueOf(sigar.getPid());
		this.procMemInfoList = new ArrayList<>();
	}

	@Override
    public void terminate() {
        running = false;
    }

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		while (running) {
			try {
				Thread.sleep(GlobalVariables.MONITOR_FREQUENCY);
				Map<String, String> map = new HashMap<String, String>(sigar.getProcMem(pid).toMap());
				procMemInfoList.add(Long.valueOf(map.get("PageFaults")));
				procMemInfoList.add(Long.valueOf(map.get("Resident")));
			} catch (InterruptedException | SigarException ex) {
				// For now, ignore any exceptions.
			}
		}
	}

	@Override
	public void print(String prefix) throws IOException {
		String outputFilename = prefix + ".memory.data";
		File outputFile = new File(outputFilename);

		/* Create the file, if not exists. */
		if (!outputFile.exists())
			outputFile.createNewFile();

		PrintWriter outputWriter = new PrintWriter(new FileOutputStream(outputFilename, true));
	
		outputWriter.println("SequenceID,PageFaults,Resident");
		for (int i = 0; i < procMemInfoList.size(); i += 2)
			outputWriter.println((i / 2) + "," + procMemInfoList.get(i) + "," + procMemInfoList.get(i + 1));

		outputWriter.close();
	}
}
