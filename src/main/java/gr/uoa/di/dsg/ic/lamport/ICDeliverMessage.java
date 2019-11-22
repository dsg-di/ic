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
package gr.uoa.di.dsg.ic.lamport;

import gr.uoa.di.dsg.communicator.GenericMessageType;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.utils.StringUtils;

import java.util.List;

public class ICDeliverMessage extends Message {
	public static GenericMessageType type = SynchronousICMessageType.ICDELIVER;
	
	private String[] values = null;
	
	public ICDeliverMessage(String ICID, String[] values) {
		super(ICID);
		this.values = values;
	}
	
	public String[] getValues() {
		return values;
	}
	
	public List<String> getValuesAsList() {
		return StringUtils.arrayToList(values);
	}

	@Override
	public int getType() {
		return type.getValue();
	}
	
	@Override
	public byte[] serialize() {
		throw new UnsupportedOperationException("The method serialize is not applicable to ICDeliverMessage objects!");
	}

	@Override
	public String toString() {
		return "ICDeliverMessage <" + applicationID + ", " + StringUtils.arrayToString(values) + ">";
	}
}
