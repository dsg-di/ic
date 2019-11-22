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
package gr.uoa.di.dsg.ic;

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.broadcast.bracha.BrachaBroadcast;
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcast;
import gr.uoa.di.dsg.broadcast.multicast.Multicast;
import gr.uoa.di.dsg.broadcast.reliable.ReliableBroadcastWithSignatures;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.NettyCommunicator;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.crypto.CryptographyModule;
import gr.uoa.di.dsg.crypto.DigitalSignatureCryptographyModule;
import gr.uoa.di.dsg.crypto.MacCryptographyModule;
import gr.uoa.di.dsg.ic.bracha.BBInteractiveConsistency;
import gr.uoa.di.dsg.ic.experiments.ExperimentCompleteMessage;
import gr.uoa.di.dsg.ic.experiments.ExperimentInformationMessage;
import gr.uoa.di.dsg.ic.experiments.ExperimentInformationReceivedMessage;
import gr.uoa.di.dsg.ic.experiments.ExperimentStartMessage;
import gr.uoa.di.dsg.ic.experiments.ExperimentTerminationMessage;
import gr.uoa.di.dsg.ic.experiments.ExperimentsMessageType;
import gr.uoa.di.dsg.ic.experiments.ICBaseSlaveParallel;
import gr.uoa.di.dsg.ic.lamport.SynchronousIC;
import gr.uoa.di.dsg.ic.lamport.SynchronousICBroadcast;
import gr.uoa.di.dsg.ic.multivalued.MVInteractiveConsistency;
import gr.uoa.di.dsg.ic.recovery.RecoveryMessageType;
import gr.uoa.di.dsg.ic.recovery.RecoveryRequestMessage;
import gr.uoa.di.dsg.ic.recovery.RecoveryResponseMessage;
import gr.uoa.di.dsg.monitoring.CPUMonitor;
import gr.uoa.di.dsg.monitoring.MemoryMonitor;
import gr.uoa.di.dsg.monitoring.Monitor;
import gr.uoa.di.dsg.monitoring.NetworkMonitor;
import gr.uoa.di.dsg.utils.GlobalVariables;
import gr.uoa.di.dsg.utils.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hyperic.sigar.Sigar;

public class ICMainExecutorParallel extends ICBaseSlaveParallel {

	private static ICMainExecutorParallel icMainExe = null;

	private class ICNode extends Node{		
		private String ip = null;
		private int port = 0;
		private String nodeGroup = null;
		
		public ICNode(int id, String ip, int port, String nodeGroup) {
			super(id);
			this.ip = ip;
			this.port = port;
			this.nodeGroup = nodeGroup;
		}
		
		public ICNode(int id, String ip, int port, String keyStoreFilename, String keyStorePwd, String symmetricKeyStorePath, String nodeGroup) {
			super(id, keyStoreFilename, keyStorePwd, symmetricKeyStorePath);
			this.ip = ip;
			this.port = port;
			this.nodeGroup = nodeGroup;
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
			return new String[] {nodeGroup};
		}
	}

	private IBroadcast constBroadcast = null;
	private IBroadcast relBroadcast = null;
	private ICResult resultProcessor = null;
	private ICFirstPhase firstPhaseProcessor = null;
	private ApplicationGetter applicationGetter = null;

	private Node[] nodes = null;

	private int warmupCounter = 0;
	private int instanceCounter = 0;
	
	private Map<String, Application> activeApps = new HashMap<>();

	/* Variables related to monitoring. */
	private String monitoringMode;
	private Sigar sigar;
	private Monitor monitoringRunnable = null;
	private Thread monitoringThread = null;

	public ICMainExecutorParallel(int nodeId) {
		this.nodeId = nodeId;
		this.sigar = new Sigar();
	}
	
	private void instantiateNodes(String configurationFile) {
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
		this.nodes = new Node[icMainExe.N + 1];
		
		// Instantiate all ICWorker nodes.
		for (int i = 0; i < icMainExe.N; i++) {
			String ip = props.getProperty("Node" + (i+1) + ".Address");
			Integer port = Integer.valueOf(props.getProperty("Node" + (i+1) + ".Port"));
			String keyStorePath = props.getProperty("keyStorePath");
			String keyStorePwd = props.getProperty("keyStorePasswd");
			String symmetricKeyStorePath = props.getProperty("symmetricKeyStore");
			
			if(ip == null || port == null || keyStorePath == null || keyStorePwd == null)
				throw new RuntimeException("The configuration is not correct!");

			this.nodes[i] = new ICNode(i, ip, port, keyStorePath, keyStorePwd, symmetricKeyStorePath, GlobalVariables.ICWORKERS_GROUP);
		}
		
		// Instantiate the ICMaster node.
		String ip = props.getProperty("ICMaster.Address");
		Integer port = Integer.valueOf(props.getProperty("ICMaster.Port"));
		
		if(ip == null || port == null)
			throw new RuntimeException("The configuration is not correct!");

		this.nodes[icMainExe.N] = new ICNode(icMainExe.N, ip, port, GlobalVariables.ICMASTER_GROUP);
	}

	private void exchangeHelloMessages() {
//		try {
//			if(nodeId == 0)
//				System.out.println("[Node: " + nodeId + "]: Sleeping for 20 seconds...");
//			
//			Thread.sleep(20000);
//			
//			if(nodeId == 0)
//				System.out.println("[Node: " + nodeId + "]: Woke up!");
//		}
//		catch (InterruptedException ex) {
//			System.err.println("(1): An InterruptedException was caught: " + ex.getMessage());
//			ex.printStackTrace();
//		}
//		
//		try {
//			HelloMessage helloMsg = new HelloMessage();
//			icMainExe.comm.sendGroup(GlobalVariables.ICWORKERS_GROUP, helloMsg);
//			
//			if(nodeId == 0)
//				System.out.println("[Node: " + nodeId + "]: Sleeping again for 10 seconds...");
//			
//			Thread.sleep(10000);
//			
//			if(nodeId == 0)
//				System.out.println("[Node: " + nodeId + "]: Woke up!");
//		}
//		catch (InterruptedException ex) {
//			System.err.println("(2): An InterruptedException was caught: " + ex.getMessage());
//			ex.printStackTrace();
//		}
		
		System.out.println("[Node: " + nodeId + "]: Starting the warmup phase...");
	}
	
	@Override
	public void executeTest() {
		// Submit an empty task to trigger the communicator's internal methods.
		comm.start( () -> {});
	}

	public void run() {
		// Contact with all IC nodes.
		if(inWarmupPhase)
			exchangeHelloMessages();
		else {
			try {
				TimeUnit.MILLISECONDS.sleep(rendezvousTime - System.currentTimeMillis());
			}
			catch (InterruptedException ex) {
				System.err.println("An InterruptedException was caught: " + ex.getMessage());
				ex.printStackTrace();
				System.exit(-1);
			}
		}
		
		int applicationID = totalExecutedOperations - operationsInEachExecution;
		for (int i = 0; i < operationsInEachExecution; ++i, ++applicationID) {
			
			Application app = applicationGetter.getApp(String.valueOf(applicationID));
			if(!inWarmupPhase)
				markStartTime(applicationID - warmupRounds + 1);
			
			app.start(this.nodeId, Integer.toString(applicationID * this.N + this.nodeId));
		}
	}
	
	public void processResult(String applicationID, List<String> res) {
		if(!inWarmupPhase)
			markDataPoint(Integer.valueOf(applicationID) - warmupRounds + 1);
		else
			System.out.println("Completing interactive consistency " + applicationID + " for node " + this.nodeId + " [" + StringUtils.listToString(res) + "]");
		
		/* Verify the values of the IC vector. */
		/*for(int i = 0; i < this.N; ++i) {
			if(res.get(i) == null || res.get(i).equalsIgnoreCase("null"))
				continue;
			
			String expectedResult = String.valueOf(((this.N * Integer.valueOf(applicationID)) + i));
			if(!expectedResult.equalsIgnoreCase(res.get(i)))
				System.out.println("WARNING: The IC vector for applicationID " + applicationID + " is incorrect in index: " + i + ", [" + StringUtils.listToString(res) + "]");
		}*/
		
		if(inWarmupPhase) {
			if(++warmupCounter == warmupRounds) {
				// Notify the ICMaster process that I have terminated my execution.
				comm.send(nodes[this.N], new ExperimentCompleteMessage());
				
				System.out.println("[Node " + nodeId + "]: Completed all instances of IC in the warm-up phase...");
			}
		}
		else {
			if(++instanceCounter == this.K) {
				// Terminate the monitoring thread.
				if (monitoringRunnable != null)
					monitoringRunnable.terminate();

				// Append the results to the corresponding output file.
				appendTimeToFile();

				if (monitoringRunnable != null) {
					/*
					 * First, join the monitoring thread and then, create the output file that will contain all monitoring data; all the
					 * necessary directories will have already been created by the above call to the appendTimeToFile() function.
					 */
					try {
						monitoringThread.join();
						monitoringRunnable.print(new String(outputDirectory + "/" + N + "." + K + "." + nodeId + "." + algorithm + ".parallel"));
					} catch (InterruptedException ex) {
						System.err.println("Could not join the monitoring thread: " + ex.getMessage());
					} catch (IOException ex) {
						System.err.println("Error while persisting all monitoring data due to: " + ex.getMessage());
					}
				}

				// Notify the ICMaster process that I have terminated my execution.
				comm.send(nodes[this.N], new ExperimentCompleteMessage());

				System.out.println("[Node " + nodeId + "]: Completed all instances of IC...");
			}
		}
	}

	public void processFirstPhaseCompletion(String applicationID) {
		if(!inWarmupPhase)
			markFirstPhaseEndTime(Integer.valueOf(applicationID) - warmupRounds + 1);
	}

	private Application getICBApplication(String appID) {
		Application app = activeApps.get(appID);
		if (app == null) {
			app = new BBInteractiveConsistency(appID, this.nodeId, this.N, this.relBroadcast, this.constBroadcast, this.resultProcessor, this.firstPhaseProcessor, GlobalVariables.ICWORKERS_GROUP);
			activeApps.put(appID, app);
		}
		return app;
	}
	
	private Application getICMApplication(String appID) {
		Application app = activeApps.get(appID);
		if (app == null) {
			app = new MVInteractiveConsistency(appID, this.nodeId, this.N, this.constBroadcast, this.relBroadcast, this.resultProcessor, this.firstPhaseProcessor, GlobalVariables.ICWORKERS_GROUP);
			activeApps.put(appID, app);
		}
		return app;
	}
	
	private Application getSynchronousICApplication(String appID) {
		Application app = activeApps.get(appID);
		if (app == null) {
			app = new SynchronousIC(appID, this.relBroadcast, resultProcessor);
			activeApps.put(appID, app);
		}
		
		return app;
	}
	
	private void processExperimentInformationMessage(ExperimentInformationMessage experimentInformationMsg, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Node " + nodeId + "]: An ExperimentInformationMessage was received " + experimentInformationMsg.toString() + " from Node " + source.getNodeId());
		
		warmupRounds = experimentInformationMsg.getWarmupRounds();
		outputDirectory = experimentInformationMsg.getOutputDirectory();

		firstPhaseDuration = new double[K + 1];
		timeValues = new double[K + 1];
		for (int i = 0; i < timeValues.length; ++i)
			timeValues[i] = -1.0;
		
		System.out.println("ICBaseSlave: Slave " + nodeId + " started. Will execute " + warmupRounds + " warm-up and " + K + " " + executionMode + " instances of the " + algorithm + " algorithm.");
		
		// Respond to ICMaster.
		//comm.send(nodes[N], new ExperimentInformationReceivedMessage());
		comm.sendGroup(GlobalVariables.ICWORKERS_GROUP, new ExperimentInformationReceivedMessage(comm.getCurrentNode().getNodeId()));
	}
	
	private void processExperimentStartMessage(ExperimentStartMessage experimentStartMsg, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Node " + nodeId + "]: An ExperimentStartMessage was received " + experimentStartMsg.toString() + " from Node " + source.getNodeId());
		
		//Get the value of the current iteration and the rendezvous time.
		operationsInEachExecution = experimentStartMsg.getOperationsInEachExecution();
		totalExecutedOperations += operationsInEachExecution;
		rendezvousTime = experimentStartMsg.getRendezvousTime();
		inWarmupPhase = experimentStartMsg.inWarmupPhase();
		
		if(warmupCounter < warmupRounds) {
			run();
		}
		else {
			if (instanceCounter == 0) {
				if ("CPU".equalsIgnoreCase(monitoringMode)) {
					/* Start a new thread that monitors the CPU consumption. */
					monitoringRunnable = new CPUMonitor(sigar);
				} else if ("Memory".equalsIgnoreCase(monitoringMode)) {
					/* Start a new thread that monitors the memory consumption. */
					monitoringRunnable = new MemoryMonitor(sigar);
				} else if ("Network".equalsIgnoreCase(monitoringMode)) {
					/* Start a new thread that monitors the network traffic. */
					monitoringRunnable = new NetworkMonitor(sigar);
				}

				if (monitoringRunnable != null) {
					System.out.println("[Node " + nodeId + "]: Launching the monitoring thread...");
					monitoringThread = new Thread(monitoringRunnable);
					monitoringThread.start();
				}
			}

			if(instanceCounter < K) {
				run();
			}
		}
	}
        
	private void processExperimentInformationReceivedMessage(ExperimentInformationReceivedMessage msg, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Node " + nodeId + "]: An ExperimentInformationReceivedMessage was received " + msg.toString() + " from Node " + source.getNodeId());
                comm.sendGroup(GlobalVariables.ICMASTER_GROUP, msg);
	}
	
	private void processExperimentTerminationMessage(ExperimentTerminationMessage experimentTerminationMsg, Node source) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[Node " + nodeId + "]: An ExperimentTerminationMessage was received from Node " + source.getNodeId());
		
		this.comm.stop();
		System.exit(0);
	}

	public static void main(String[] args) {
		if(args.length < 11 || args.length > 13) {
			System.out.println("Usage: <nodeID> <totalNodes> <num_of_instances> <execution> <configuration_file> <isCrashed> <timeout> <useAuthenticators> "
					+ "<enable-asynchronous-work> <data-size-in-bytes> [High] [Low]");
			System.exit(-1);
		}
		
		int nodeID = Integer.parseInt(args[0]);
		icMainExe = new ICMainExecutorParallel(nodeID);
		
		icMainExe.N = Integer.parseInt(args[1]);
		icMainExe.K = Integer.parseInt(args[2]);

		boolean hasCrashed = Boolean.parseBoolean(args[5]);
		
		String configuration = args[3];
		GlobalVariables.TIMEOUT = Integer.parseInt(args[6]);
		
		/* Check whether to use digital signatures or authenticators. */
		boolean useAuthenticators = Boolean.parseBoolean(args[7]);
		CryptographyModule cryptoModule = null;
		
		if(useAuthenticators)
			cryptoModule = new MacCryptographyModule();
		else
			cryptoModule = new DigitalSignatureCryptographyModule();
		
		icMainExe.instantiateNodes(args[4]);
		icMainExe.resultProcessor = (String appId, List<String> res)-> icMainExe.processResult(appId, res);
		icMainExe.firstPhaseProcessor = (String appId) -> icMainExe.processFirstPhaseCompletion(appId);
		
		//icMainExe.comm = new SSLThreadedCommunicator(icMainExe.nodes[nodeID], icMainExe.nodes);
		//icMainExe.comm = new TcpCommunicator(icMainExe.nodes[nodeID], icMainExe.nodes);
		//icMainExe.comm = new NettyCommunicator(icMainExe.nodes[nodeID], icMainExe.nodes);
		icMainExe.comm = new NettyCommunicator(icMainExe.nodes[nodeID], icMainExe.nodes, 
				"Certificates/node" + (nodeID) + "/node" + (nodeID) + "P8.key.pem", 
				"Certificates/node" + (nodeID) + "/node" + (nodeID) + ".crt.pem", 
				"Certificates/node" + (nodeID) + "/node" + (nodeID) + "cP8.key.pem", 
				"Certificates/node" + (nodeID) + "/node" + (nodeID) + "c.crt.pem", 
				"Certificates/node" + (nodeID) + "/ca.crt.pem",  "password");
		if( System.getenv("ENABLE_LOG").equals("YES"))
			icMainExe.comm.enableLog();

		//icMainExe.comm.setTransmitDelay(transmissionDelay);
		if(hasCrashed)
			icMainExe.comm.setCrashed();
		
		GlobalVariables.ENABLE_ASYNC_WORK = Boolean.parseBoolean(args[8]);
		
		icMainExe.monitoringMode = args[10];
		
		/* Check for debugging flags. */
		if(args.length == 12) {
			if(args[11].equalsIgnoreCase("High"))
				GlobalVariables.HIGH_VERBOSE = true;
			else if(args[11].equalsIgnoreCase("Low"))
				GlobalVariables.LOW_VERBOSE = true;
			else {
				System.err.println("Unknown option: " + args[11]);
				System.exit(-1);
			}
		}
		
		if(args.length == 13) {
			if(!args[11].equalsIgnoreCase("High") && !args[11].equalsIgnoreCase("Low")) {
				System.err.println("Unknown option: " + args[11]);
				System.exit(-1);
			}
			
			if(!args[12].equalsIgnoreCase("High") && !args[12].equalsIgnoreCase("Low")) {
				System.err.println("Unknown option: " + args[12]);
				System.exit(-1);
			}
			
			GlobalVariables.HIGH_VERBOSE = true;
			GlobalVariables.LOW_VERBOSE = true;
		}
		
		switch (configuration) {
			case "MVBB":
				icMainExe.applicationGetter = (String appID)->icMainExe.getICMApplication(appID);
				icMainExe.constBroadcast = new Multicast();
				icMainExe.relBroadcast = new BrachaBroadcast();
				icMainExe.constBroadcast.initialize(icMainExe.comm, icMainExe.applicationGetter, GlobalVariables.ICWORKERS_GROUP, icMainExe.N);
				icMainExe.relBroadcast.initialize(icMainExe.comm, icMainExe.applicationGetter, GlobalVariables.ICWORKERS_GROUP, icMainExe.N);
				break;
			case "MVRB":
				icMainExe.applicationGetter = (String appID)->icMainExe.getICMApplication(appID);
				icMainExe.constBroadcast = new Multicast();
				icMainExe.relBroadcast = new ReliableBroadcastWithSignatures(cryptoModule);
				icMainExe.constBroadcast.initialize(icMainExe.comm, icMainExe.applicationGetter, GlobalVariables.ICWORKERS_GROUP, icMainExe.N);
				icMainExe.relBroadcast.initialize(icMainExe.comm, icMainExe.applicationGetter, GlobalVariables.ICWORKERS_GROUP, icMainExe.N);
				break;
			case "BCBB":
				icMainExe.applicationGetter = (String appID)->icMainExe.getICBApplication(appID);
				icMainExe.constBroadcast = new ConsistentBroadcast(cryptoModule);
				icMainExe.relBroadcast = new BrachaBroadcast();
				icMainExe.constBroadcast.initialize(icMainExe.comm, icMainExe.applicationGetter, GlobalVariables.ICWORKERS_GROUP, icMainExe.N);
				icMainExe.relBroadcast.initialize(icMainExe.comm, icMainExe.applicationGetter, GlobalVariables.ICWORKERS_GROUP, icMainExe.N);
				break;
			case "BCRB":
				icMainExe.applicationGetter = (String appID)->icMainExe.getICBApplication(appID);
				icMainExe.constBroadcast = new ConsistentBroadcast(cryptoModule);
				icMainExe.relBroadcast = new ReliableBroadcastWithSignatures(cryptoModule);
				icMainExe.constBroadcast.initialize(icMainExe.comm, icMainExe.applicationGetter, GlobalVariables.ICWORKERS_GROUP, icMainExe.N);
				icMainExe.relBroadcast.initialize(icMainExe.comm, icMainExe.applicationGetter, GlobalVariables.ICWORKERS_GROUP, icMainExe.N);
				break;
			case "LAMP":
				icMainExe.applicationGetter = (String appID) -> icMainExe.getSynchronousICApplication(appID);
				icMainExe.relBroadcast = new SynchronousICBroadcast();
				icMainExe.relBroadcast.initialize(icMainExe.comm, icMainExe.applicationGetter, GlobalVariables.ICWORKERS_GROUP, icMainExe.N);
				break;
			default:
				System.out.println("Unknown configuration: " + configuration);
				System.exit(-1);
		}
		
		icMainExe.algorithm = configuration;
		icMainExe.executionMode = "parallel";
		
		// Register all necessary handlers for the communication with the ICMaster.
		icMainExe.comm.registerMessage(ExperimentsMessageType.EXP_INFORMATION.getValue(), (byte[] data) -> ExperimentInformationMessage.deserialize(data), (Message msg, Node source) -> icMainExe.processExperimentInformationMessage((ExperimentInformationMessage) msg, source));
		icMainExe.comm.registerMessage(ExperimentsMessageType.EXP_START.getValue(), (byte[] data) -> ExperimentStartMessage.deserialize(data), (Message msg, Node source) -> icMainExe.processExperimentStartMessage((ExperimentStartMessage) msg, source));
		icMainExe.comm.registerMessage(ExperimentsMessageType.EXP_TERMINATION.getValue(), (byte[] data) -> ExperimentTerminationMessage.deserialize(data), (Message msg, Node source) -> icMainExe.processExperimentTerminationMessage((ExperimentTerminationMessage) msg, source));

                //also register handlers for sent messages so that debugging works
                icMainExe.comm.registerMessage(ExperimentsMessageType.EXP_INFORMATION_RECEIVED.getValue(), (byte[] data) -> ExperimentInformationReceivedMessage.deserialize(data), (Message msg, Node source) -> icMainExe.processExperimentInformationReceivedMessage((ExperimentInformationReceivedMessage)msg, source));
		icMainExe.comm.registerMessage(ExperimentsMessageType.EXP_COMPLETE.getValue(), (byte[] data) -> ExperimentCompleteMessage.deserialize(data), (Message msg, Node source) -> {throw new UnsupportedOperationException();});

		// Register all necessary handlers for the recovery phase.
		icMainExe.comm.registerMessage(RecoveryMessageType.REQUEST.getValue(), (byte[] data) -> RecoveryRequestMessage.deserialize(data),
				(Message msg, Node source) -> {
					BBInteractiveConsistency app = (BBInteractiveConsistency) icMainExe.applicationGetter.getApp(msg.getApplicationID());
					app.processRecoveryRequestMessage((RecoveryRequestMessage) msg, source);
				});
				
		icMainExe.comm.registerMessage(RecoveryMessageType.RESPONSE.getValue(), (byte[] data) -> RecoveryResponseMessage.deserialize(data),
				(Message msg, Node source) -> {
					BBInteractiveConsistency app = (BBInteractiveConsistency) icMainExe.applicationGetter.getApp(msg.getApplicationID());
					app.processRecoveryResponseMessage((RecoveryResponseMessage) msg, source);
				});
		
		icMainExe.executeTest();
	}
}
