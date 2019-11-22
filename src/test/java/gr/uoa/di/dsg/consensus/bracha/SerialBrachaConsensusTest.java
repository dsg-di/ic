package gr.uoa.di.dsg.consensus.bracha;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.broadcast.bracha.BrachaBroadcast;
import gr.uoa.di.dsg.communicator.AbstractTestNode;
import gr.uoa.di.dsg.communicator.DummyCommunicator;
import gr.uoa.di.dsg.communicator.EndMessage;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.ic.bracha.ICChunkMessage;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class SerialBrachaConsensusTest {

	private int numNodes = 11;
	private int numDead = 3;
	private AbstractTestNode[] allNodes = new AbstractTestNode[numNodes];
	private static int TotalSerialConsensusInstances = 5;
	
	private class TestNode extends AbstractTestNode implements Application {
		
		public TestNode(int nodeId, int port) {
			super(nodeId, port, "src/main/resources/keyStores", "password");
			this.communicator = new DummyCommunicator(this, allNodes);
		}
		
		private Map<Integer, BBConsensus> activeConsensus = new HashMap<>();
		private Map<Integer, String> completedConsensus = new HashMap<>();
		private IBroadcast relBroadcast = new BrachaBroadcast();
		
		private int counter = 0;
		
		public void run() {
			
			relBroadcast.initialize(communicator, (String appID) -> getApplication(appID), GlobalVariables.ICWORKERS_GROUP, allNodes.length);
			communicator.start( ()->init() );
		}

		public void init(){

			BBConsensus cons = new BBConsensus(this, "0", this.nodeId, counter, numNodes);
			activeConsensus.put(counter, cons);
			if(this.nodeId > 3)
				cons.start("1");
			else
				cons.start("0");
		}
		
		public Application getApplication(String appID)
		{
			return this;
		}

		@Override
		public void process(Message msg) {
			
			if(!(msg instanceof BroadcastAccept) )
				throw new IllegalArgumentException("IC.process not applicable for argument "+msg.getClass());
			
			
			BroadcastAccept acc = (BroadcastAccept) msg;
			
			if(completedConsensus.containsKey(acc.getConsensusID()))
				return;
				
			BBConsensus cons = activeConsensus.get(acc.getConsensusID());
			if(cons == null){
				cons = new BBConsensus(this, "0", this.nodeId, acc.getConsensusID(), numNodes);
				activeConsensus.put(acc.getConsensusID(), cons);
			}
			cons.process(acc);
		}

		@Override
		public void processConsensusResult(int cid, String value) {
			System.out.println("["+cid+"] Consensus Completed for process = "+this.nodeId+" with value = "+value);
			completedConsensus.put(cid, value);
			activeConsensus.remove(cid);
			counter++;
			
			if (counter < TotalSerialConsensusInstances) {
				BBConsensus cons = new BBConsensus(this, "0", this.nodeId, counter, numNodes);
				activeConsensus.put(counter, cons);
				if(this.nodeId > 3)
					cons.start("2");
				else
					cons.start("1");
			}
			else {
				this.communicator.send(this.communicator.getCurrentNode(), new EndMessage());
			}
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
		for(int i=0; i<numNodes - numDead; i++) {
			threads[i] = new Thread(allNodes[i]);
		}
		
		for(int i=0; i< numNodes - numDead; i++)
			threads[i].start();
		
		for(int i=0; i<numNodes - numDead; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Too fast termination...");
	}

}
