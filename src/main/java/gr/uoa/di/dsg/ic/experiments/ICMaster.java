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

import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.communicator.IdentityMessage;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.NettyCommunicator;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ICMaster {
	
	private final static boolean VERBOSE = true;
	private static int WARMUP_ROUNDS = 3;
	private static int RENDEZVOUS_TIME = 10_000;
	private static ICMaster icMaster = null;
	
	private class ICNode extends Node {		
		private String ip = null;
		private int port = 0;
		private String group = null;
		
		public ICNode(int id, String ip, int port, String group) {
			super(id);
			this.ip = ip;
			this.port = port;
			this.group = group;
		}
		
		@Override
		public InetAddress getAddress() {
			try {
				return InetAddress.getByName(ip);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public int getPort() {
			return port;
		}
		
		@Override
		public String[] getGroups() {
			return new String[] {group};
		}
	}
	
	private Node[] nodes = null;
	private int nodeId = -1;
	
	private int N;
	private int K;
	private String algorithm;
	private String outputDir;
	private String modeOfExecution;
	private AbstractCommunicator comm = null;
	
	private int warmupCounter = 0;
	private int actualExecutionsCounter = 0;
	
	private int informationReceivedMessages = 0;
	private Set<Integer> experimentCompleteIDs = new HashSet<>();
        
	
	private static String usage() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Usage: java -jar ICMaster.jar <N> <K> <algorithm> <outputFolder> <executionMode> <configurationFile> [warmupRounds] [rendezvousTime]\n");
		builder.append("\t<N> is the number of replicas\n");
		builder.append("\t<K> is the number of algorithm instances that will be run\n");
		builder.append("\t<algorithm> is the algorithm that the slaves will run (MVBB,MVRB,BCBB,BCRB)\n");
		builder.append("\t<outputFolder> is the directory where the log file must be placed\n");
		builder.append("\t<executionMode> is the mode of execution and must belong in {serial, parallel}\n");
		builder.append("\t<configurationFile> is the configuration file that contains connection information for every node\n");
		builder.append("\t[warmupRounds] (optional) is the number of warm-up rounds\n");
		builder.append("\t[rendezvousTime] (optional)is the rendezvous time in seconds\n");
		
		return builder.toString();
	}
	
	public ICMaster() {}
	
	private void instantiateNodes(int totalNodes, String configurationFile) {
		/* Load the configuration file. */
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(configurationFile));
		}
		catch(IOException ex) {
			System.err.println("An IOException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		// Allocate memory space for the ICMaster as well.
		this.nodes = new Node[totalNodes + 1];
		
		// Instantiate all ICWorker nodes.
		for (int i = 0; i < totalNodes; i++) {
			String ip = props.getProperty("Node" + (i+1) + ".Address");
			Integer port = Integer.valueOf(props.getProperty("Node" + (i+1) + ".Port"));
			
			if(ip == null || port == null)
				throw new RuntimeException("The configuration is not correct!");

			this.nodes[i] = new ICNode(i, ip, port, GlobalVariables.ICWORKERS_GROUP);
		}
		
		// Instantiate the ICMaster node.
		String ip = props.getProperty("ICMaster.Address");
		Integer port = Integer.valueOf(props.getProperty("ICMaster.Port"));
		
		if(ip == null || port == null)
			throw new RuntimeException("The configuration is not correct!");

		this.nodes[totalNodes] = new ICNode(totalNodes, ip, port, GlobalVariables.ICMASTER_GROUP);
	}

	public static void main(String args[]) {
		if (args.length < 6 || args.length > 8) {
			System.err.println("Invalid program invocation!\n" + usage());
			System.exit(-1);
		}
		
		// Initialize the static instance of the ICMaster class.
		icMaster = new ICMaster();
		
		icMaster.N = Integer.valueOf(args[0]);
		if (icMaster.N < 1) {
			System.err.println("Master: Invalid value specified for program paratemer n. Specified value " + icMaster.N + " is not bigger or equal than 1.");
			System.exit(-1);
		}
		
		icMaster.K = Integer.valueOf(args[1]);
		if (icMaster.K < 1) {
			System.err.println("Master: Invalid value specified for program paratemer k. Specified value " + icMaster.K + " is not bigger or equal than 1.");
			System.exit(-1);
		}
		
		icMaster.algorithm = args[2];
		if (!icMaster.algorithm.equals("MVBB") && !icMaster.algorithm.equals("MVRB") && !icMaster.algorithm.equals("BCBB") && !icMaster.algorithm.equals("BCRB") && !icMaster.algorithm.equals("LAMP")) {
			System.err.println("Master: Invalid value specified for the algorithm that will be executed!");
			System.exit(-1);
		}
		
		icMaster.outputDir = args[3];
		if (icMaster.outputDir == null) {
			System.err.println("Master: The output directory must be explicitly specified!");
			System.exit(-1);
		}

		icMaster.modeOfExecution = args[4].toLowerCase();
		if (!icMaster.modeOfExecution.equals("serial") && !icMaster.modeOfExecution.equals("parallel")) {
			System.err.println("Unknown execution mode: " + args[4]);
			System.exit(-1);
		}
		
		// Update the number of warm-up rounds.
		if(args.length > 6)
			WARMUP_ROUNDS = Integer.valueOf(args[6]);
		
		// Update the rendezvous time.
		if(args.length == 8)
			RENDEZVOUS_TIME = Integer.valueOf(args[7]) * 1000;
		
		icMaster.instantiateNodes(icMaster.N, args[5]);
		icMaster.nodeId = icMaster.N;
		icMaster.comm = new NettyCommunicator(icMaster.nodes[icMaster.nodeId], icMaster.nodes, 
				"Certificates/node" + (icMaster.nodeId) + "/node" + (icMaster.nodeId) + "P8.key.pem", 
				"Certificates/node" + (icMaster.nodeId) + "/node" + (icMaster.nodeId) + ".crt.pem", 
				"Certificates/node" + (icMaster.nodeId) + "/node" + (icMaster.nodeId) + "cP8.key.pem", 
				"Certificates/node" + (icMaster.nodeId) + "/node" + (icMaster.nodeId) + "c.crt.pem", 
				"Certificates/node" + (icMaster.nodeId) + "/ca.crt.pem",  "password");

		System.out.println("env ENABLE_LOG=" + System.getenv("ENABLE_LOG"));
		if( System.getenv("ENABLE_LOG").equals("YES")) {
			icMaster.comm.enableLog();
			System.out.println("enableLog() called");
		}
		// Register all necessary handlers.
		icMaster.comm.registerMessage(ExperimentsMessageType.EXP_INFORMATION_RECEIVED.getValue(), (byte[] data) -> ExperimentInformationReceivedMessage.deserialize(data), (Message msg, Node source) -> icMaster.processExperimentInformationReceivedMessage((ExperimentInformationReceivedMessage) msg, source));
		icMaster.comm.registerMessage(ExperimentsMessageType.EXP_COMPLETE.getValue(), (byte[] data) -> ExperimentCompleteMessage.deserialize(data), (Message msg, Node source) -> icMaster.processExperimentCompleteMessage((ExperimentCompleteMessage) msg, source));
		
                //also register handlers for sent messages so that debugging works
		icMaster.comm.registerMessage(ExperimentsMessageType.EXP_INFORMATION.getValue(), (byte[] data) -> ExperimentInformationMessage.deserialize(data), (Message msg, Node source) -> {throw new UnsupportedOperationException();});
		icMaster.comm.registerMessage(ExperimentsMessageType.EXP_START.getValue(), (byte[] data) -> ExperimentStartMessage.deserialize(data), (Message msg, Node source) -> {throw new UnsupportedOperationException();});
		icMaster.comm.registerMessage(ExperimentsMessageType.EXP_TERMINATION.getValue(), (byte[] data) -> ExperimentTerminationMessage.deserialize(data), (Message msg, Node source) -> {throw new UnsupportedOperationException();});
                
		icMaster.comm.start( ()-> icMaster.sendExperimentInformation() );
	}
	
	private void sendExperimentInformation() {
		System.out.println("Master: Sending the experiment's information to all nodes...");
		comm.sendGroup(GlobalVariables.ICWORKERS_GROUP, new IdentityMessage(comm.getCurrentNode().getNodeId()));
		comm.sendGroup(GlobalVariables.ICWORKERS_GROUP, new ExperimentInformationMessage(WARMUP_ROUNDS, outputDir));
		System.out.println("Master: Waiting for all nodes to respond...");
	}

	private void processExperimentCompleteMessage(ExperimentCompleteMessage msg, Node source) {
		if(VERBOSE)
			System.out.println("Master: An ExperimentCompleteMessage was received from Node " + source.getNodeId());
		
		if( (icMaster.experimentCompleteIDs.add(source.getNodeId())) && icMaster.experimentCompleteIDs.size() == icMaster.N) {
			if(icMaster.warmupCounter < ICMaster.WARMUP_ROUNDS) {
				if (icMaster.modeOfExecution.equalsIgnoreCase("serial"))
					startWarmupExecution(1);
				else
					startWarmupExecution(WARMUP_ROUNDS);
				
				// Clear the completed nodeIDs.
				icMaster.experimentCompleteIDs.clear();
			}
			else {
				if(icMaster.actualExecutionsCounter == 0) {
					try {
						System.out.println("Master: The warmup phase is over...");
						TimeUnit.SECONDS.sleep(3);
						System.out.println("Master: Starting the actual executions...");
					}
					catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
				
				if(icMaster.actualExecutionsCounter < icMaster.K) {
					if (icMaster.modeOfExecution.equalsIgnoreCase("serial"))
						startActualExecution(1);
					else
						startActualExecution(icMaster.K);
					
					// Clear the completed nodeIDs.
					icMaster.experimentCompleteIDs.clear();
				}
				else {
					comm.sendGroup(GlobalVariables.ICWORKERS_GROUP, new ExperimentTerminationMessage());
					System.out.println("Master: Successfully waited for all slaves!\nExiting normally...");
					
					try {
						TimeUnit.SECONDS.sleep(3);
					}
					catch(InterruptedException ex) {
						System.err.println("An InterruptedException was caught: " + ex.getMessage());
						System.exit(-1);
					}
					
					this.comm.stop();
					System.exit(0);
				}
			}
		}
	}

	private void processExperimentInformationReceivedMessage(ExperimentInformationReceivedMessage msg, Node source) {
		if(VERBOSE)
			System.out.println("Master: An ExperimentInformationReceivedMessage was received from Node " + source.getNodeId());
		informationReceivedMessages += 1;
		if( informationReceivedMessages == (icMaster.N * icMaster.N)) {
			// Start the warm-up executions.
			System.out.println("Master: Starting the warm-up phase...");
			
			if (icMaster.modeOfExecution.equalsIgnoreCase("serial"))
				startWarmupExecution(1);
			else
				startWarmupExecution(WARMUP_ROUNDS);
		}
	}
	
	private void startWarmupExecution(int operationsInEachExecution) {
		warmupCounter += operationsInEachExecution;
		comm.sendGroup(GlobalVariables.ICWORKERS_GROUP, new ExperimentStartMessage(true, operationsInEachExecution, System.currentTimeMillis() + RENDEZVOUS_TIME));
	}
	
	private void startActualExecution(int operationsInEachExecution) {
		actualExecutionsCounter += operationsInEachExecution;
		comm.sendGroup(GlobalVariables.ICWORKERS_GROUP, new ExperimentStartMessage(false, operationsInEachExecution, System.currentTimeMillis() + RENDEZVOUS_TIME));
	}
}
