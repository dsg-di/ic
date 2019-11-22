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

public class GlobalVariables {
	public static int TIMEOUT = 3;
	public static boolean HIGH_VERBOSE = false;
	public static boolean LOW_VERBOSE = false;
	public static boolean ENABLE_ASYNC_WORK = false;
	public static final String DIGEST_ALGORITHM = "SHA-256";
	public static final String ICWORKERS_GROUP = "ICWorkers";
	public static final String ICMASTER_GROUP = "ICMaster";
	public static final int DATUM_CHUNK_SIZE = 60 * 1024;
	public static final int MONITOR_FREQUENCY = 50; // milliseconds.
}
