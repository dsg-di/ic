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
package gr.uoa.di.dsg.broadcast;

import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.ic.ApplicationGetter;

/**
 * @author Panos
 * @version 0.0.1 An interface that describes the functionality of a
 *          <i>broadcast primitive</i> It allows the creation of different
 *          variations of broadcast primitives.
 */
public interface IBroadcast {
	
	/**
	 * The initialize function is invoked <em>before</em> the system invokes the
	 * broadcast function to disseminate a message. It initialize a series of
	 * structures that meant to keep the messages participating in the protocol
	 * running in the broadcast layer. It is highly <em>recommended</em> to use
	 * this function for registering a series of message handlers to the
	 * communicator for the proper message dispatching.
	 * <p>
	 * For an example of implementation see
	 * {@link gr.uoa.di.dsg.broadcast.bracha.BrachaBroadcast}
	 * 
	 * @param comm
	 *            The Communicator that handles the dispatching of messages (
	 *            {@link gr.uoa.di.dsg.communicator.AbstractCommunicator})
	 * @param appGetter
	 *            A function that when given an application Id it returns the
	 *            object representing that Id (
	 *            {@link gr.uoa.di.dsg.ic.ApplicationGetter})
	 * @param nodeGroup
	 * @param numNodes
	 */
	public void initialize(AbstractCommunicator comm, ApplicationGetter appGetter, String nodeGroup, int numNodes);


	/**
	 * The broadcast function utilizes the communicator and is used for the
	 * dissemination of values from a sender to all other recipient nodes. As
	 * the name states, the message containing that value will be delivered to
	 * <em>all</em> nodes in the system
	 * 
	 * @param icid
	 *            The application id that invokes the consensus.
	 * @param cid
	 *            The consensus id that invokes the broadcast for that value
	 * @param pid
	 *            The node id that corresponds to the sender identifier of the
	 *            message
	 * @param bid
	 *            The broadcast id that uniquely describes the broadcast
	 *            regarding the specific application and consensus
	 * @param value
	 *            The value to be disseminated
	 */
	public void broadcast(String icid, int cid, int pid, int bid, String value);

	/**
	 * The getCommunicator function is used by the layers of Consensus and
	 * Application to access the communicator instance. This allows each layer to
	 * tamper with the configuration of the Communicator.
	 * 
	 * @return The instance of Communicator that is used for message dispatching
	 */
	public AbstractCommunicator getCommunicator();
}
