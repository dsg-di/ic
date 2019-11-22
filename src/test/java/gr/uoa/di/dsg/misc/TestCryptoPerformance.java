package gr.uoa.di.dsg.misc;

import gr.uoa.di.dsg.crypto.CryptographyModule;
import gr.uoa.di.dsg.crypto.DigitalSignatureCryptographyModule;
import gr.uoa.di.dsg.crypto.MacCryptographyModule;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;

public class TestCryptoPerformance {

	private List<SecretKey> secretKeys = new ArrayList<>();
	private List<KeyPair> keyPairs = new ArrayList<>();

	private int MIN = 4;
	private static int MAX = 20;
	private int REPETITIONS = 200;
	
	@Before
	public void before() throws NoSuchAlgorithmException {
		KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
		gen.initialize(1024);
		KeyGenerator secretGen = KeyGenerator.getInstance("HmacSha1");
		
		for (int i = 0; i < MAX; ++i) {
			/* Generate a symmetric key. */
			secretKeys.add(secretGen.generateKey());

			/* Generate a key pair. */
			keyPairs.add(i, gen.generateKeyPair());
		}
		
		if(System.getProperty("total") != null)
			MAX = Integer.valueOf(System.getProperty("total"));
	}
	
	@Test
	public void test() throws NoSuchAlgorithmException {
		byte[] array = new byte[256];
		new Random().nextBytes(array);
		String data = new String(array);
		
		for(int total = 0; total < 5; ++total) {
			for(int ite = MIN; ite <= MAX; ++ite) {
				CryptographyModule module = new DigitalSignatureCryptographyModule();
				byte[] signature = null;
				
				for(int j = 0; j < REPETITIONS; ++j)
					signature = module.sign(data, keyPairs.get(0).getPrivate());
				
				for(int j = 0; j < REPETITIONS; ++j)
					assert module.verify(data, keyPairs.get(0).getPublic(), signature);
				
				module = new MacCryptographyModule();
				List<byte[]> signatures = null;
				
				for(int j = 0; j < REPETITIONS; ++j) {
					signatures = new ArrayList<>();
					for(int i = 0; i < ite; ++i)
						signatures.add(module.sign(data, secretKeys.get(i)));
				}
	
				for(int j = 0; j < REPETITIONS; ++j)
					assert module.verify(data, secretKeys.get(0), signatures.get(0));
			}
		}
		
		for(int ite = MIN; ite <= MAX; ++ite) {
			System.out.println("Starting simulation using " + ite + " nodes...");
			
			CryptographyModule module = new DigitalSignatureCryptographyModule();
			List<byte[]> digitalSignatures = new ArrayList<>();
			
			long start = System.currentTimeMillis();
			for(int j = 0; j < REPETITIONS; ++j)
				digitalSignatures.add(module.sign(data, keyPairs.get(0).getPrivate()));
			long end = System.currentTimeMillis();
			System.out.println("Signature generation: " + ((end - start) / (double) REPETITIONS) + " milliseconds!");
			
			start = System.currentTimeMillis();
			for(int j = 0; j < REPETITIONS; ++j)
				assert module.verify(data, keyPairs.get(0).getPublic(), digitalSignatures.get(j));
			end = System.currentTimeMillis();
			System.out.println("Signature verification: " + ((end - start) / (double) REPETITIONS) + " milliseconds!");
			
			module = new MacCryptographyModule();
			List<byte[]> signatures = null;
			
			start = System.currentTimeMillis();
			for(int j = 0; j < REPETITIONS; ++j) {
				signatures = new ArrayList<>();
				for(int i = 0; i < ite; ++i)
					signatures.add(module.sign(data, secretKeys.get(i)));
			}
			end = System.currentTimeMillis();
			System.out.println("Mac generation: " + ((end - start) / (double) REPETITIONS) + " milliseconds!");
			
			start = System.currentTimeMillis();
			for(int j = 0; j < REPETITIONS; ++j)
				assert module.verify(data, secretKeys.get(0), signatures.get(0));
			end = System.currentTimeMillis();
			System.out.println("Mac verification: " + ((end - start) / (double) REPETITIONS) + " milliseconds!\n");
		}
	}

	public static void main(String[] args) throws Exception {
		if(args.length == 1)
			MAX = Integer.valueOf(args[0]);
		
		JUnitCore.main("gr.uoa.di.dsg.misc.TestCryptoPerformance");            
	}
}
