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

import java.util.ArrayList;
import java.util.List;

public class StringUtils {
	
	private final static String DELIM = ",";
	
	public static String arrayToString(String[] values) {
		String str = "";
		
		for(int i = 0; i < values.length - 1; ++i)
			str += (values[i] + ",");
		
		str += values[values.length - 1];
		
		return str;
	}
	
	public static String[] stringToArray(String str) {
		return str.split(DELIM);
	}
	
	public static String listToString(List<String> list) {
		String str = "";
		
		if(list.size() <= 0)
			return str;
		
		for(int i = 0; i < list.size() - 1; ++i)
			str += (list.get(i) + ",");
		
		str += list.get(list.size() - 1);
		
		return str;
	}
	
	public static List<String> arrayToList(String[] array) {
		if(array == null)
			return new ArrayList<>(0);
		
		List<String> list = new ArrayList<>(array.length);
		
		for(int i = 0; i < array.length; ++i)
			list.add(array[i]);
		
		return list;
	}
}
