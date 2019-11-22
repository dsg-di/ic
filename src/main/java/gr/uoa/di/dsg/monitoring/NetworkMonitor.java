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
import java.util.Map.Entry;

import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import gr.uoa.di.dsg.utils.GlobalVariables;

/**
 * 
 * The current implementation is based on the following posts:
 * -- https://stackoverflow.com/questions/47715082/java-sigar-getting-network-traffic
 * -- https://stackoverflow.com/questions/10999392/how-to-get-cpu-ram-and-network-usage-of-a-java7-app
 *
 */
public class NetworkMonitor implements Monitor {

	private Sigar sigar;
	private volatile boolean running = true;
	private List<Long> receivedBytesList;
	private List<Long> transmittedBytesList;

	private Map<String, Long> receivedCurrent = new HashMap<String, Long>();
	private Map<String, List<Long>> receivedDiff = new HashMap<String, List<Long>>();
    private Map<String, Long> transmittedCurrent = new HashMap<String, Long>();
    private Map<String, List<Long>> transmittedDiff = new HashMap<String, List<Long>>();

	public NetworkMonitor(Sigar sigar) {
		this.sigar = sigar;
		this.receivedBytesList = new ArrayList<>();
		this.transmittedBytesList = new ArrayList<>();

		this.receivedCurrent = new HashMap<String, Long>();
		this.receivedDiff = new HashMap<String, List<Long>>();
		this.transmittedCurrent = new HashMap<String, Long>();
		this.transmittedDiff = new HashMap<String, List<Long>>();
	}

	@Override
    public void terminate() {
        running = false;
    }

	@Override
	public void run() {
		long total_received_bytes = 0;
		long total_transmitted_bytes = 0;

		while (running) {
			try {
				Thread.sleep(GlobalVariables.MONITOR_FREQUENCY);

	            Long[] m = getMetric();
	            total_received_bytes += m[0];
	            total_transmitted_bytes += m[1];

	            receivedBytesList.add(total_received_bytes);
	            transmittedBytesList.add(total_transmitted_bytes);

			} catch (InterruptedException | SigarException ex) {
				// For now, ignore any exceptions.
			}
		}
	}

	@Override
	public void print(String prefix) throws IOException {
		try {
			System.out.println(getNetworkInfo());
		} catch(SigarException ex) {
			// For now, ignore the exception.
		}

		String outputFilename = prefix + ".network.data";
		File outputFile = new File(outputFilename);

		/* Create the file, if not exists. */
		if (!outputFile.exists())
			outputFile.createNewFile();

		PrintWriter outputWriter = new PrintWriter(new FileOutputStream(outputFilename, true));

		outputWriter.println("SequenceID,Received,Transmitted");
		for (int i = 0; i < receivedBytesList.size(); ++i)
			outputWriter.println((i + 1) + "," + receivedBytesList.get(i) + "," + transmittedBytesList.get(i));
		outputWriter.close();
	}

	private String getNetworkInfo() throws SigarException {
        return sigar.getNetInfo().toString() + "\n"+ sigar.getNetInterfaceConfig().toString();
    }

	public Long[] getMetric() throws SigarException {
        for (String ni: sigar.getNetInterfaceList()) {
            NetInterfaceStat netInterfaceStat = sigar.getNetInterfaceStat(ni);
            NetInterfaceConfig netInterfaceConfig = sigar.getNetInterfaceConfig(ni);
            String hwAddr = null;

            if (!NetFlags.NULL_HWADDR.equals(netInterfaceConfig.getHwaddr()))
                hwAddr = netInterfaceConfig.getHwaddr();

            if (hwAddr != null) {
                store_diff(receivedCurrent, receivedDiff, hwAddr, netInterfaceStat.getRxBytes(), ni);
                store_diff(transmittedCurrent, transmittedDiff, hwAddr, netInterfaceStat.getTxBytes(), ni);
            }
        }

        return new Long[] {calculate_diff(receivedDiff), calculate_diff(transmittedDiff)};
    }

    private long calculate_diff(Map<String, List<Long>> rxChangeMap) {
        long total = 0;

        for (Entry<String, List<Long>> entry: rxChangeMap.entrySet()) {
            total += entry.getValue().stream().mapToLong(Long::longValue).sum() / entry.getValue().size();
            entry.getValue().clear();
        }

        return total;
    }

    private void store_diff(Map<String, Long> currentMap, Map<String, List<Long>> changeMap, String hwaddr, long current, String ni) {
        Long old = currentMap.get(ni);
        if (old != null) {
            List<Long> list = changeMap.get(hwaddr);
            if (list == null) {
                list = new ArrayList<Long>();
                changeMap.put(hwaddr, list);
            }
            list.add((current - old));
        }
        currentMap.put(ni, current);
    }
}
