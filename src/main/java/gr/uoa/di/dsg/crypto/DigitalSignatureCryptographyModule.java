/*******************************************************************************
 * Copyright (C) 2019 DSG at University of Athens
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package gr.uoa.di.dsg.crypto;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class DigitalSignatureCryptographyModule implements CryptographyModule {

	private final static String ALGORITHM = "SHA1withRSA";

	@Override
	public byte[] sign(String data, Key key) {
		return sign(data.getBytes(), key);
	}
	
	@Override
	public byte[] sign(byte[] data, Key key) {
		Signature signSignature = null;
		
		try {
			signSignature = Signature.getInstance(ALGORITHM);
			signSignature.initSign((PrivateKey) key);
			signSignature.update(data);
			
			return signSignature.sign();
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
			System.err.println("An exception " + ex.getClass() + " was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return null;
	}

	@Override
	public boolean verify(String data, Key key, byte[] signature) {
		return verify(data.getBytes(), key, signature);
	}
	
	@Override
	public boolean verify(byte[] data, Key key, byte[] signature) {
		Signature verifySignature = null;
		
		try {
			/* Verify the validity of the received message. */
			verifySignature = Signature.getInstance(ALGORITHM);
			verifySignature.initVerify((PublicKey) key);
			verifySignature.update(data);
			
			return verifySignature.verify(signature);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
			System.err.println("An exception " + ex.getClass() + " was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return false;
	}

	@Override
	public int getDigestLength() {
		throw new RuntimeException("Operation not supported!");
	}

}
