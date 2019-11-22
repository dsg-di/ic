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
package gr.uoa.di.dsg.broadcast.consistent;

import gr.uoa.di.dsg.communicator.GenericMessageType;

/**
 * An enumeration that distinguishes the different type of messages
 * exchanged during the execution of an instance of the {@link ConsistentBroadcast}
 * or the {@link ReliableBroadcast} protocols.
 * 
 * The type of each message must be a unique number during an application's
 * execution. Each type is described in detail:
 * <ul>
 * 		<li>CBSEND - represents a {@link CBSendMessage}, which is sent during the first round of both protocols.</li>
 * 		<li>CBECHO - represents a {@link CBEchoMessage}, which is sent during the second round of both protocols.</li>
 * 		<li>CBFINAL - represents a {@link CBFinalMessage}, which is sent during the third round of the {@link ConsistentBroadcast} protocol.</li>
 *		<li>CBDELIVER - represents a {@link BroadcastAccept} message, which is used to notify the application about the final value delivered by the protocol.</li> 
 * </ul>
 * 
 * @author stathis
 *
 */

public enum BrodacastWithSignaturesMessageType implements GenericMessageType
{
	CBSEND(10),
	CBECHO(11),
	CBFINAL(12),
	CBDELIVER(13),
	RBSSEND(14),
	RBSECHO(15),
	RBSFINAL(16);

	int value;
	BrodacastWithSignaturesMessageType(int value){
		this.value  = value;
	}
	
	@Override
	public int getValue() {
		return value;
	}
}
