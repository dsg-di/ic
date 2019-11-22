package gr.uoa.di.dsg.ic.lamport;

import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.AbstractTestNode;
import gr.uoa.di.dsg.communicator.DummyCommunicator;
import gr.uoa.di.dsg.communicator.EndMessage;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.ic.ICResult;
import gr.uoa.di.dsg.ic.bracha.ICChunkMessage;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TestSerialSynchronousIC {

	private int numNodes = 4;
	private AbstractTestNode[] allNodes = new AbstractTestNode[numNodes];
	private final int TOTAL_EXECUTIONS = 3;

	private class TestNode extends AbstractTestNode implements Application {
		
		private Map<String, SynchronousIC> activeSynchronousICs = new HashMap<>();
		private Map<String, List<String>> results = new HashMap<>();
		
		private String value = null;
		
		private SynchronousICBroadcast broadcastModule = new SynchronousICBroadcast();
		
		private ICResult resultProcessor = (String appID, List<String> res)-> processResult(appID, res);
		
		private String currentICID = "1";
		
		public TestNode(int nodeId, int port) {
			super(nodeId, port);
			value = String.valueOf(nodeId);
			this.communicator = new DummyCommunicator(this, allNodes);
		}

		private void processResult(String appID, List<String> res) {
			activeSynchronousICs.remove(appID);
			results.put(appID, res);
			
			System.out.println("SynchronousIC: " + appID + ", result: " + Arrays.asList(res));
			
			if(results.size() == TOTAL_EXECUTIONS) {
				/*End execution. */
				this.communicator.send(this.communicator.getCurrentNode(), new EndMessage());
			}
			else {
				currentICID = String.valueOf(Integer.valueOf(currentICID) + 1);
				this.init();
			}
		}

		public void run() {
			broadcastModule.initialize(communicator, (String appID) -> getApplication(appID), GlobalVariables.ICWORKERS_GROUP, allNodes.length);
			communicator.start(() -> init());
		}

		public void init() {
			SynchronousIC synchronousIC = new SynchronousIC(currentICID, broadcastModule, resultProcessor);
			activeSynchronousICs.put(currentICID, synchronousIC);
			synchronousIC.start(nodeId, value);
		}

		public Application getApplication(String appID) {
			return this;
		}

		@Override
		public void process(Message msg) {
			SynchronousIC instance = activeSynchronousICs.get(msg.getApplicationID());
			instance.process(msg);
		}

		@Override
		public void processConsensusResult(int cid, String value) {
			throw new RuntimeException("Operation not supported!");
		}

		@Override
		public IBroadcast getBroadcast() {
			return broadcastModule;
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
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
