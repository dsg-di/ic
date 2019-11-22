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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class SecretKeysGenerator {

	public static void generateSecretKeys(String path, int totalNodes) throws IOException, NoSuchAlgorithmException {
		/* Create the appropriate directories. */
		File file = new File(path);
		if (!file.exists())
			file.mkdirs();

		for (int i = 0; i < totalNodes; ++i) {
			for (int j = i; j < totalNodes; ++j) {
				/* Generate a symmetric key. */
				KeyGenerator keyGen = KeyGenerator.getInstance("HmacSha1");
				SecretKey secretKey = keyGen.generateKey();

				String filename = path + "key" + i + "_" + j;
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename, false));
				out.writeObject(secretKey);
				out.close();

				filename = path + "key" + j + "_" + i;
				out = new ObjectOutputStream(new FileOutputStream(filename, false));
				out.writeObject(secretKey);
				out.close();
			}
		}
	}
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
		if(args.length != 2) {
			System.err.println("Usage: <path> <totalNodes>");
			System.exit(-1);
		}
		
		int nodes = Integer.valueOf(args[1]).intValue();
		
		/* Generate symmetric keys. */
		SecretKeysGenerator.generateSecretKeys(args[0], nodes);
	}
}
