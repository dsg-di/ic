package gr.uoa.di.dsg.broadcast.bracha;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for testing the proper serializability of exchanged messages.
 */
public class TestMessageSerializability extends TestCase {
	
	/** Constants and variables for testing. */
	@SuppressWarnings("unused")
	private final int TOTAL_NODES = 4;
	
	private final String IC_ID = "1";
	private final int CONSENSUS_ID = 1;
	private final int BROADCAST_ID = 1;
	private final int PROCESS_ID = 0;
	
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
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     * @throws SignatureException 
     */
    public void testApp() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    	Random random = new Random();
    	
    	/* Create a random integer value inside the interval [0, 9]. */
    	String sampleValue = String.valueOf(random.nextInt(10));
    	
    	EchoMessage echoMessage = new EchoMessage(IC_ID, CONSENSUS_ID, PROCESS_ID, BROADCAST_ID, sampleValue);
    	EchoMessage copyOfEchoMessage = EchoMessage.deserialize(echoMessage.serialize());

    	InitMessage initMessage = new InitMessage(IC_ID, CONSENSUS_ID, PROCESS_ID, BROADCAST_ID, sampleValue);
    	InitMessage copyOfInitMessage = InitMessage.deserialize(initMessage.serialize());
    	
    	ReadyMessage readyMessage = new ReadyMessage(IC_ID, CONSENSUS_ID, PROCESS_ID, BROADCAST_ID, sampleValue);
    	ReadyMessage copyOfReadyMessage = ReadyMessage.deserialize(readyMessage.serialize());
    	
    	assert initMessage.equals(copyOfInitMessage);
    	assert echoMessage.equals(copyOfEchoMessage);
    	assert readyMessage.equals(copyOfReadyMessage);
    	
    	System.out.println("Success!");
    }
}
