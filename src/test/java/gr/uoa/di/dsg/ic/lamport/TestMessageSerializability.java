package gr.uoa.di.dsg.ic.lamport;

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
	private final int TOTAL_NODES = 4;
	private final String IC_ID = "1";
	private final int CONSENSUS_ID = 1;
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
    	
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    	List<byte[]> signatures = new ArrayList<>(TOTAL_NODES);
    	List<String> values = new ArrayList<>(TOTAL_NODES);
    	
    	for(int i = 0; i < TOTAL_NODES; ++i) {
    		/* Create a random integer value inside the interval [0, 9]. */
    		String sampleValue = String.valueOf(random.nextInt(10));
    		values.add(sampleValue);
    		
    		KeyPair pair = keyGen.generateKeyPair();
    		Signature signSignature = Signature.getInstance("SHA1withRSA");
    		signSignature.initSign(pair.getPrivate());
    		signSignature.update(sampleValue.getBytes());
    		
    		signatures.add(signSignature.sign());   		
    	}
    	
    	String value = "Test";
    	ICInitMessage initMessage = new ICInitMessage(IC_ID, CONSENSUS_ID, PROCESS_ID, value);
    	ICInitMessage copyOfInitMessage = ICInitMessage.deserialize(initMessage.serialize());
    	
    	ICVectorMessage vectorMessage = new ICVectorMessage(IC_ID, CONSENSUS_ID, PROCESS_ID);
    	vectorMessage.setValues(values);
    	
    	ICVectorMessage copyOfVectorMessage = ICVectorMessage.deserialize(vectorMessage.serialize());
    	
    	assert initMessage.isEqual(copyOfInitMessage);
    	assert vectorMessage.isEqual(copyOfVectorMessage);
    	
    	System.out.println("Success!");
    }
}
