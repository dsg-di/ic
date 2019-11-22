package gr.uoa.di.dsg.ic.bracha;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.broadcast.bracha.BrachaBroadcast;
import gr.uoa.di.dsg.broadcast.consistent.ConsistentBroadcast;
import gr.uoa.di.dsg.communicator.AbstractTestNode;
import gr.uoa.di.dsg.communicator.DummyCommunicator;
import gr.uoa.di.dsg.communicator.NettyCommunicator;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.ic.ApplicationGetter;
import gr.uoa.di.dsg.ic.ICFirstPhase;
import gr.uoa.di.dsg.ic.ICResult;
import gr.uoa.di.dsg.ic.torrent.ICTorrentMessage;
import gr.uoa.di.dsg.utils.GlobalVariables;

public class TestForWatchdog {

	int numNodes = 4;
	TestNode allNodes[] = null;
	
	private class TestNode extends AbstractTestNode {
		
		public TestNode(int nodeId, int port) {
			super(nodeId, port);
			this.communicator = new NettyCommunicator(this, allNodes);
			
			
			//initialize torrent logic communicator messages
			this.communicator.registerMessage(ICTorrentMessage.messageType, 
					(data) -> ICTorrentMessage.deserialize(data), 
					(msg, source) -> processTorrentMessage((ICTorrentMessage) msg, source));
			
		}

		@Override
		public void run() {
			 communicator.start( ()->init() );
		}
		
		private IBroadcast constBroadcast = null;
		private IBroadcast relBroadcast = null;
		private ICResult resultProcessor = null;
		private ICFirstPhase firstPhaseProcessor = null;
		private ApplicationGetter applicationGetter = null;
		private int instancesCounter = 0;
		private Map<String, Application> activeApps = new HashMap<>();
		
		public void init() {
			Application app = applicationGetter.getApp(String.valueOf(instancesCounter));
			app.start(this.nodeId, Integer.toString(instancesCounter * numNodes + this.nodeId) );
			//second attribute of start is the value the nodes have to agree on
			//on watchdog case this is the folder content hash
		}
		
		public void processResult(String appID, List<String> res) {
			System.out.println("Completing interactive consistency " + appID + " for node " + this.nodeId + " : ");

			for (String ress : res)
				System.out.print(ress + " ");
			System.out.println();
			
			this.activeApps.remove(appID);
			this.communicator.stop();
		}
		
		public void processFirstPhaseCompletion(String appID) {}
		
		public Application getICBApplication(String appID) {
			Application app = activeApps.get(appID);
			if (app == null) {
				app = new BBInteractiveConsistency(appID, this.nodeId, numNodes, this.relBroadcast, this.constBroadcast, this.resultProcessor, this.firstPhaseProcessor, GlobalVariables.ICWORKERS_GROUP);
				activeApps.put(appID, app);
			}
			return app;
		}
		
		public void initialize() {
			resultProcessor = (String appId, List<String> res) -> processResult(appId, res);
			firstPhaseProcessor = (String appId) -> processFirstPhaseCompletion(appId);
			communicator = new DummyCommunicator(this, allNodes);
			applicationGetter = (String appID) -> getICBApplication(appID);
			constBroadcast = new ConsistentBroadcast();
			relBroadcast = new BrachaBroadcast();
			constBroadcast.initialize(communicator, applicationGetter, GlobalVariables.ICWORKERS_GROUP, allNodes.length);
			relBroadcast.initialize(communicator, applicationGetter, GlobalVariables.ICWORKERS_GROUP, allNodes.length);
		}
	}
	
	
	public void processTorrentMessage(ICTorrentMessage msg , 	Node source){
		
		
	}
	
	@Before
	public void setUp() throws Exception {
		allNodes = new TestNode[numNodes];
		for( int i = 0; i < numNodes; i++ ) {
			allNodes[i] = new TestNode(i, 3000+i);
			((TestNode) allNodes[i]).initialize();
		}
		
	}

	@Test
	public void test() {
		
		Thread[] threads = new Thread[numNodes];
		for(int i=0; i<numNodes; i++) {
			threads[i] = new Thread(allNodes[i]);
		}
		
		for(int i=0; i<numNodes; i++) {
			threads[i].start();
		}
		
		for(int i=0; i<numNodes; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
