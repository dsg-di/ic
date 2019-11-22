package gr.uoa.di.dsg.broadcast.bracha;

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.AbstractTestNode;
import gr.uoa.di.dsg.communicator.DummyCommunicator;
import gr.uoa.di.dsg.communicator.EndMessage;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.ic.bracha.ICChunkMessage;
import gr.uoa.di.dsg.utils.GlobalVariables;

import org.junit.Before;
import org.junit.Test;

public class BrachaBroadcastTest {
	private int numNodes = 4;
	private AbstractTestNode[] allNodes = new AbstractTestNode[numNodes];
	
	private class TestNode extends AbstractTestNode implements Application {
		
		public TestNode(int nodeId, int port) {
			super(nodeId, port);
			this.communicator = new DummyCommunicator(this, allNodes);
		}
		
		private String value = "A";
		private BrachaBroadcast relBroadcast = new BrachaBroadcast();
		
		public void run() {
			relBroadcast.initialize(communicator, (String appID)->getApplication(appID), GlobalVariables.ICWORKERS_GROUP, allNodes.length);
			communicator.start( ()->init() );
		}

		public void init(){
			relBroadcast.broadcast("0", 0, this.nodeId, 0, value);
		}
		
		public Application getApplication(String appID)
		{
			return this;
		}

		@Override
		public void process(Message msg) {
			this.communicator.send(this.communicator.getCurrentNode(), new EndMessage());
		}

		@Override
		public void processConsensusResult(int cid, String value) {
		}

		@Override
		public IBroadcast getBroadcast() {
			return relBroadcast;
		}

		@Override
		public void start(int nodeID, String value) {
		}

		@Override
		public void start() {
		}
		
		@Override
		public void processDatumChunk(ICChunkMessage icChunkMessage) {
		}
		
		@Override
		public boolean verifyDatum(String value, int sourceNodeID) {
			throw new RuntimeException("Operation not supported");
		}
	}
	
	@Before
	public void setUp() throws Exception {
		for( int i = 0; i < numNodes; i++ ) {
			allNodes[i] = new TestNode(i, 3000+i);
		}
	}

	@Test
	public void testHappyPath() {
		Thread[] threads = new Thread[numNodes];
		for(int i=0; i<numNodes; i++) {
			threads[i] = new Thread(allNodes[i]);
			threads[i].start();
		}
		
		for(int i=0; i<numNodes; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Too fast termination...");
	}

}
