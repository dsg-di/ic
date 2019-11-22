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
package gr.uoa.di.dsg.utils;

public class GlobalUtils {

    /**
     * It computes the threshold for the given number of nodes
     * @param n The number of nodes
     * @return The threshold for the given number
     */
    public static int computeThreshold(int n) {
    	int faults = (int) Math.floor((n - 1) / 3.0);
    	
    	return n - faults;
    }
    
    /**
     * Converts a byte array to a hex String.
     * @param bytes, the byte array to convert.
     * @return a hex representation of the byte array.
     */
    public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        
        return new String(hexChars);
    }
    
    /**
     * Converts a Hex string to a byte array
     * @param s, the string to convert.
     * @return a byte array that contains the string.
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));

        return data;
    }
}
