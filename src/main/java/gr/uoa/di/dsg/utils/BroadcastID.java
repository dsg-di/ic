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

public enum BroadcastID {
	MVINITIAL_BROADCAST_ID(-1),
	MVINIT_BROADCAST_ID(-2),
	MVVECTOR_BROADCAST_ID(-3),
	MVACCEPT_BROADCAST_ID(-4),
	MVPROPOSE_BROADCAST_ID(-5),
	CB_BROADCAST_ID(-6),
	RB_BROADCAST_ID(-7),
	IC_BROADCAST_ID(-8);
	
	private int value;
	
	BroadcastID(int value) {
		this.value  = value;
	}
	
	public int getValue() {
		return value;
	}
}
