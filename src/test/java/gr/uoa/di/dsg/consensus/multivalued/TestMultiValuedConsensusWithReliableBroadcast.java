package gr.uoa.di.dsg.consensus.multivalued;

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.broadcast.multicast.Multicast;
import gr.uoa.di.dsg.broadcast.reliable.ReliableBroadcastWithSignatures;
import gr.uoa.di.dsg.communicator.AbstractTestNode;
import gr.uoa.di.dsg.communicator.DummyCommunicator;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.ic.ApplicationGetter;
import gr.uoa.di.dsg.ic.ICFirstPhase;
import gr.uoa.di.dsg.ic.ICResult;
import gr.uoa.di.dsg.ic.multivalued.MVInteractiveConsistency;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TestMultiValuedConsensusWithReliableBroadcast {

	private int numNodes = 4;
	private TestNode allNodes[] = null;

	private class TestNode extends AbstractTestNode {

		public TestNode(int nodeId, int port) {
			super(nodeId, port);
			this.communicator = new DummyCommunicator(this, allNodes);
		}

		@Override
		public void run() {
			communicator.start(() -> init());
		}

		private IBroadcast reliableBroadcast = null;
		private IBroadcast multicastModule = null;
		private ICResult resultProcessor = null;
		private ICFirstPhase firstPhaseProcessor = null;
		private ApplicationGetter applicationGetter = null;
		private int instancesCounter = 0;
		private int total = 3;

		private Map<String, Application> activeApps = new HashMap<>();

		public void init() {
			Application app = applicationGetter.getApp(String.valueOf(instancesCounter));
			app.start(this.nodeId, Integer.toString(instancesCounter * numNodes + this.nodeId));
		}

		public void processResult(String appID, List<String> res) {
			System.out.println("Completing interactive consistency: " + appID + " for node: " + this.nodeId + " with vector: " + Arrays.asList(res).toString());
			
			this.activeApps.remove(appID);
			
			--total;
			if(total > 0) {
				Application app = applicationGetter.getApp(String.valueOf(Integer.valueOf(appID) + 1));
				app.start(this.nodeId, Integer.toString(((++instancesCounter - 1) * numNodes) + this.nodeId));
			}
			else
				this.communicator.stop();
		}
		
		public void processFirstPhaseCompletion(String appID) {}

		public Application getMVICApplication(String appID) {
			Application app = activeApps.get(appID);
			if (app == null) {
				app = new MVInteractiveConsistency(appID, this.nodeId, numNodes, multicastModule, reliableBroadcast, resultProcessor, firstPhaseProcessor, GlobalVariables.ICWORKERS_GROUP);
				activeApps.put(appID, app);
			}
			
			return app;
		}

		public void initialize() {
			this.resultProcessor = (String appId, List<String> res) -> processResult(appId, res);
			this.firstPhaseProcessor = (String appId) -> processFirstPhaseCompletion(appId);
			this.communicator = new DummyCommunicator(this, allNodes);
			this.applicationGetter = (String appID) -> getMVICApplication(appID);
			
			this.multicastModule = new Multicast();
			this.multicastModule.initialize(communicator, applicationGetter, GlobalVariables.ICWORKERS_GROUP, allNodes.length);
			
			this.reliableBroadcast = new ReliableBroadcastWithSignatures();
			this.reliableBroadcast.initialize(communicator, applicationGetter, GlobalVariables.ICWORKERS_GROUP, allNodes.length);
		}
	}

	@Before
	public void setUp() throws Exception {
		allNodes = new TestNode[numNodes];
		
		for (int i = 0; i < numNodes; i++) {
			allNodes[i] = new TestNode(i, 3000+i);
			((TestNode) allNodes[i]).initialize();
		}
	}

	@Test
	public void test() {
		Thread[] threads = new Thread[numNodes];
		
		for (int i = 0; i < numNodes; i++)
			threads[i] = new Thread(allNodes[i]);

		for (int i = 0; i < numNodes; i++)
			threads[i].start();

		for (int i = 0; i < numNodes; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
