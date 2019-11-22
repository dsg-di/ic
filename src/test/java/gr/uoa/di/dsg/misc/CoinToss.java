package gr.uoa.di.dsg.misc;

import java.security.SecureRandom;

import org.junit.Before;
import org.junit.Test;

public class CoinToss {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		System.out.println(coin_toss());
	}
	
	private String coin_toss(){
		SecureRandom random = new SecureRandom();
//		byte[] seed = random.generateSeed(16);
//		random.setSeed(seed);
		return ""+random.nextInt(2)+"";
	}

}
