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
package gr.uoa.di.dsg.ic.experiments;

import gr.uoa.di.dsg.communicator.GenericMessageType;

public enum ExperimentsMessageType implements GenericMessageType
{
	/* Important: The value of EXP_INFORMATION_RECEIVED and EXP_COMPLETE messages
	 * must be greater or equal than 100. Otherwise, the corresponding messages
	 * will never reach their destination, if a node is crashed.
	 */
	
	EXP_INFORMATION(100),
	EXP_INFORMATION_RECEIVED(101),
	EXP_START(102),
	EXP_COMPLETE(103),
	EXP_TERMINATION(104);

	int value;
	ExperimentsMessageType(int value){
		this.value  = value;
	}
	
	@Override
	public int getValue() {
		return value;
	}
}
