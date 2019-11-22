package gr.uoa.di.dsg.broadcast.consistent;

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.AbstractTestNode;
import gr.uoa.di.dsg.communicator.DummyCommunicator;
import gr.uoa.di.dsg.communicator.EndMessage;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.crypto.MacCryptographyModule;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.ic.bracha.ICChunkMessage;
import gr.uoa.di.dsg.utils.BroadcastID;
import gr.uoa.di.dsg.utils.GlobalVariables;

import org.junit.Before;
import org.junit.Test;

public class TestConsistentBroadcastWithAuthenticators {
	
	private int numNodes = 16;
	private AbstractTestNode[] allNodes = new AbstractTestNode[numNodes];
	private int TOTAL_EXECUTIONS = 5;
	
	private class TestNode extends AbstractTestNode implements Application {
		private String value = null;
		private IBroadcast broadcastModule = null;
		private int counter = 0;
		
		public TestNode(int nodeId, int port) {
			super(nodeId, port);
			this.communicator = new DummyCommunicator(this, allNodes);
			this.value = String.valueOf(nodeId);
			this.broadcastModule = new ConsistentBroadcast(new MacCryptographyModule());
		}
		
		public void run() {
			broadcastModule.initialize(communicator, (String appID)->getApplication(appID), GlobalVariables.ICWORKERS_GROUP, allNodes.length);
			communicator.start( ()->init() );
		}

		public void init() {
			for(int i = 0; i < TOTAL_EXECUTIONS; ++i)
				broadcastModule.broadcast(String.valueOf(i), 0, this.nodeId, BroadcastID.CB_BROADCAST_ID.getValue(), value);
		}
		
		public Application getApplication(String appID)
		{
			return this;
		}

		@Override
		public void process(Message msg) {
			System.out.println("[" + Thread.currentThread().getId() + "]: " + msg.toString());
			
			if((++counter) == TOTAL_EXECUTIONS * TestConsistentBroadcastWithAuthenticators.this.numNodes)
				this.communicator.send(this.communicator.getCurrentNode(), new EndMessage());
		}

		@Override
		public void processConsensusResult(int cid, String value) {
			return;
		}

		@Override
		public IBroadcast getBroadcast() {
			return broadcastModule;
		}

		@Override
		public void start(int nodeID, String value) {
			return;
		}
		
		@Override
		public void start() {
		}
		
		@Override
		public void processDatumChunk(ICChunkMessage icChunkMessage) {
		}
		
		@Override
		public boolean verifyDatum(String value, int sourceNodeID) {
			return true;
		}
	}
	
	@Before
	public void setUp() throws Exception {
		for (int i = 0; i < numNodes; i++) {
			allNodes[i] = new TestNode(i, 3000+i);
		}
	}

	@Test
	public void testHappyPath() {
		Thread[] threads = new Thread[numNodes];
		for (int i = 0; i < numNodes; i++) {
			threads[i] = new Thread(allNodes[i]);
			threads[i].start();
		}

		for (int i = 0; i < numNodes; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Too fast termination...");
	}
}
