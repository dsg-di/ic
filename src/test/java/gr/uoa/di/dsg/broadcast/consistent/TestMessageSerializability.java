package gr.uoa.di.dsg.broadcast.consistent;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for testing the proper serializability of exchanged messages.
 */
public class TestMessageSerializability extends TestCase {
	
	/** Constants and variables for testing. */
	private final int TOTAL_NODES = 8;
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
     * @throws IOException 
     */
    public void testApp() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
    	Random random = new Random();
    	
    	/* Create a random integer value inside the interval [0, 9]. */
    	String sampleValue = String.valueOf(random.nextInt(10));
    	
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    	List<byte[]> signatures = new ArrayList<>(TOTAL_NODES);
    	List<Integer> nodeIDs = new ArrayList<>(TOTAL_NODES);
    	
    	for(int i = 0; i < TOTAL_NODES; ++i) {
    		KeyPair pair = keyGen.generateKeyPair();
    		
    		Signature signSignature = Signature.getInstance("SHA1withRSA");
    		signSignature.initSign(pair.getPrivate());
    		signSignature.update(sampleValue.getBytes());
    		
    		signatures.add(signSignature.sign());
    		nodeIDs.add(i);
    	}
    	
    	CBSendMessage sendMessage = new CBSendMessage(IC_ID, CONSENSUS_ID, PROCESS_ID, BROADCAST_ID, sampleValue);
    	CBSendMessage copyOfSendMessage = CBSendMessage.deserialize(sendMessage.serialize());
    	System.out.println(sendMessage.serialize().length);
    	
    	CBEchoMessage echoMessage = new CBEchoMessage(IC_ID, CONSENSUS_ID, PROCESS_ID, BROADCAST_ID, sampleValue);
    	echoMessage.setSignature(nodeIDs, signatures);
    	CBEchoMessage copyOfEchoMessage = CBEchoMessage.deserialize(echoMessage.serialize());
    	
    	assert sendMessage.isEqual(copyOfSendMessage);
    	assert echoMessage.isEqual(copyOfEchoMessage);
    	assert echoMessage.getSignature() != null;
    	assert copyOfEchoMessage.getSignature() != null;
    	
    	System.out.println("Success!");
    }
}
