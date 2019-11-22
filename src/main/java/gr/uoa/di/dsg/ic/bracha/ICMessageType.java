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
package gr.uoa.di.dsg.ic.bracha;

import gr.uoa.di.dsg.communicator.GenericMessageType;

public enum ICMessageType implements GenericMessageType
{
	IC_CHUNK(37),
	IC_CHUNK_RECOVERY_RESPONSE(38),
	IC_TORRENT(39);

	int value;
	ICMessageType(int value) {
		this.value  = value;
	}
	
	@Override
	public int getValue() {
		return value;
	}
}
