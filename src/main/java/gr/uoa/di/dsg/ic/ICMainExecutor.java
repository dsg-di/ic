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
package gr.uoa.di.dsg.ic;


public class ICMainExecutor {
	public static void main(String[] args) {
		if(args.length < 12 || args.length > 14) {
			System.out.println("Usage: <mode> <enable-datum> <nodeID> <totalNodes> <num_of_instances> <execution> <configuration_file> <isCrashed> <timeout> "
					+ "<useAuthenticators> <enable-asynchronous-work> <data-size-in-bytes> [High] [Low]");
			System.exit(-1);
		}
		
		Boolean enableDatum = Boolean.parseBoolean(args[1]);
		
		/* Remove the first two arguments. */
		String[] arguments = new String[args.length - 2];
		System.arraycopy(args, 2, arguments, 0, arguments.length);
		
		if(args[0].equalsIgnoreCase("Parallel")) {
			if(enableDatum)
				ICMainExecutorParallelWithDatum.main(arguments);
			else
				ICMainExecutorParallel.main(arguments);
		}
		else if(args[0].equalsIgnoreCase("Serial")) {
			if(enableDatum)
				ICMainExecutorSerialWithDatum.main(arguments);
			else
				ICMainExecutorSerial.main(arguments);
		}
		else {
			System.err.println("Unknown option: " + args[0]);
			System.exit(-1);
		}
	}
}
