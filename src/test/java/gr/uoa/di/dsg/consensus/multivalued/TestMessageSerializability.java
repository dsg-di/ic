package gr.uoa.di.dsg.consensus.multivalued;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.consensus.multivalued.messages.MVInitMessage;
import gr.uoa.di.dsg.consensus.multivalued.messages.MVVectorMessage;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for testing the proper serializability of exchanged messages.
 */
public class TestMessageSerializability extends TestCase {
	
	/** Constants and variables for testing. */
	private final int TOTAL_NODES = 4;
	private final String IC_ID = "1";
	private final int CONSENSUS_ID = 1;
	private final int BROADCAST_ID = 1;
	private final int PROCESS_ID = 1;
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestMessageSerializability(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
    	return new TestSuite(TestMessageSerializability.class);
    }

    /**
     * Rigorous Test.
     */
    public void testApp() {
    	Random random = new Random();
    	
    	/* Create a random integer value inside the interval [0, 9]. */
    	String sampleValue = String.valueOf(random.nextInt(10));
    	
    	MVInitMessage initMsg = new MVInitMessage(new BroadcastAccept(IC_ID, CONSENSUS_ID, PROCESS_ID, BROADCAST_ID, sampleValue));
    	MVInitMessage copyOfInitMsg = MVInitMessage.deserialize(initMsg.serialize());
    	
    	/* Create some random integer values inside the interval [0, 9]. */
    	String[] sampleValues = new String[TOTAL_NODES];
    	for(int i = 0; i < TOTAL_NODES; ++i)
    		sampleValues[i] = String.valueOf(random.nextInt(10));

		MVVectorMessage vectorMsg = new MVVectorMessage(new BroadcastAccept(IC_ID, CONSENSUS_ID, PROCESS_ID, BROADCAST_ID, sampleValue));
		vectorMsg.setVectorOfValues(sampleValues);
    	MVVectorMessage copyOfVectorMsg = MVVectorMessage.deserialize(vectorMsg.serialize());
    	
    	assert initMsg.isEqual(copyOfInitMsg);
    	assert vectorMsg.isEqual(copyOfVectorMsg);
    	
    	System.out.println("Success!");
    }
}
