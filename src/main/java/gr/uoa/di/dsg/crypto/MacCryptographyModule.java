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
import java.util.Arrays;

import javax.crypto.Mac;

public class MacCryptographyModule implements CryptographyModule {

	private final static String ALGORITHM = "HmacSha1";
	
	@Override
	public byte[] sign(String data, Key key) {
		return sign(data.getBytes(), key);
	}
	
	@Override
	public byte[] sign(byte[] data, Key key) {
		Mac mac = null;
		
		try {
			mac = Mac.getInstance(ALGORITHM);
			mac.init(key);
			
			return mac.doFinal(data);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			System.err.println("An exception " + ex.getClass() + " was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return null;
	}

	@Override
	public boolean verify(String data, Key key, byte[] computedMac) {
		return verify(data.getBytes(), key, computedMac);
	}
	
	@Override
	public boolean verify(byte[] data, Key key, byte[] computedMac) {
		Mac mac = null;
		
		try {
			mac = Mac.getInstance(ALGORITHM);
			mac.init(key);
			
			return Arrays.equals(mac.doFinal(data), computedMac);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			System.err.println("An exception " + ex.getClass() + " was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return false;
	}

	@Override
	public int getDigestLength() {
		try {
			return Mac.getInstance(ALGORITHM).getMacLength();
		}
		catch(NoSuchAlgorithmException ex) {
			System.err.println("A NoSuchAlgorithmException was caught: " + ex.getMessage());
			ex.printStackTrace();
		}
		
		return -1;
	}

}
