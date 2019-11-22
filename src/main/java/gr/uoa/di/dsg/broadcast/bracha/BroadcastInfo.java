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
package gr.uoa.di.dsg.broadcast.bracha;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Panos
 * @version 0.0.1
 * 
 *          This strucure keeps all the necessary information regarding a
 *          specific value of a specific broadcast. More specifically, given a
 *          specific broadcast uid (bid) and a value (val), this structure holds
 *          all the messages regarding the tuple <bid, val>.
 */
public class BroadcastInfo {
	/**
	 * @deprecated The process associated with this Broadcast info
	 */
	private int processID = 0;

	/**
	 * The number of echo messages associated with a specific value
	 */
	private int numberOfEchoM = 0;

	/**
	 * The number of ready messages associated with a specific value
	 */
	private int numberOfReadyM = 0;

	/**
	 * The step that this broadcast is currently in.
	 */
	private int step = 0;

	/**
	 * The nodes that have sent echo message
	 */
	private Set<Integer> nodesSendEcho = null;

	/**
	 * The nodes that have sent ready message
	 */
	private Set<Integer> nodesSendReady = null;

	/**
	 * Initializes the data structures for a specific value.
	 * 
	 * @param processID
	 *            the identifier of the process that this structure corresponds
	 *            to
	 */
	public BroadcastInfo(int processID) {
		this.processID = processID;
		this.step = 1;
		this.nodesSendEcho = new HashSet<>();
		this.nodesSendReady = new HashSet<>();
	}

	/**
	 * 
	 * @param step
	 *            the step that the broadcast is currently in.
	 * @param processID
	 *            the identifier of the process that this structure corresponds
	 *            to
	 */
	public BroadcastInfo(int step, int processID) {
		this.processID = processID;
		this.step = step;
	}

	/**
	 * @deprecated Accessor for the process identifier
	 * @return the process identifier of the current broadcast
	 */
	public int getProcessID() {
		return processID;
	}

	/**
	 * @deprecated Mutator for the process identifier
	 * @param processID
	 *            sets the process identifier for the urrent broadcast
	 */
	public void setProcessID(int processID) {
		this.processID = processID;
	}

	/**
	 * Accessor for the number of echo messages
	 * 
	 * @return the number of nodes that have send echo message
	 */
	public int getNumberOfEchoM() {
		return numberOfEchoM;
	}

	/**
	 * @deprecated Mutator for the number of echo messages.
	 * @param numberOfEchoM
	 *            the number of echo messages.
	 */
	public void setNumberOfEchoM(int numberOfEchoM) {
		this.numberOfEchoM = numberOfEchoM;
	}

	/**
	 * Accessor for the number of ready messages.
	 * 
	 * @return the number of nodes that have sent ready message.
	 */
	public int getNumberOfReadyM() {
		return numberOfReadyM;
	}

	/**
	 * @deprecated Mutator for the number of ready messages
	 * @param numberOfReadyM
	 *            the number of ready messages.
	 */
	public void setNumberOfReadyM(int numberOfReadyM) {
		this.numberOfReadyM = numberOfReadyM;
	}

	/**
	 * Accessor for the step that the broadcast is currently in.
	 * 
	 * @return the step the broadcast is currently in.
	 */
	public int getStep() {
		return step;
	}

	/**
	 * It is used to set the step that the broadcast is currently in.
	 * 
	 * @param step
	 *            The current step of the broadcast.
	 */
	public void setStep(int step) {
		this.step = step;
	}

	/**
	 * Increases the number of Echo messages by one.
	 */
	public void increaseNumberOfEchoM() {
		this.numberOfEchoM++;
	}

	/**
	 * Decreases the number of Echo messages by one.
	 */
	public void descreaseNumberOfEchoM() {
		this.numberOfEchoM--;
	}

	/**
	 * Increases the number of Ready messages by one.
	 */
	public void increaseNumberOfReadyM() {
		this.numberOfReadyM++;
	}

	/**
	 * Decreases the number of Ready messaeges by one.
	 */
	public void descreaseNumberOfReadyM() {
		this.numberOfReadyM--;
	}

	/**
	 * Adds a sender node to the set of the nodes that have sent an Echo message
	 * regarding the tuple <bid, val>.
	 * 
	 * @param source
	 *            the identifier of the sender of an echo message.
	 * @return <em>true</em> if the set does not contain the sender of the echo
	 *         message, <em>false</em> in any other case.
	 */
	public boolean addEchoNode(Integer source) {
		return nodesSendEcho.add(source);
	}

	/**
	 * @deprecated Checks if a node that have sent an echo message is already in
	 *             the set of the senders
	 * 
	 * @param source
	 *            the identifier of the sender of an echo message.
	 * @return <em>true</em> if the set contained the sender of the message,
	 *         <em>false</em> in any other case.
	 */
	public boolean containsEchoNode(Integer source) {
		return nodesSendEcho.contains(source);
	}

	/**
	 * Removes the sender of the echo message from the set of the senders
	 * regarding the tuple <bid, val>
	 * 
	 * @param source
	 *            the identifier of the sender of an echo message.
	 * @return <em>true</em> if the set contained the sender of the message,
	 *         <em>false</em> in any other case
	 */
	public boolean removeEchoNode(Integer source) {
		return nodesSendEcho.remove(source);
	}

	/**
	 * Adds that sender of a Ready message to the set of the senders of Ready
	 * messages.
	 * 
	 * @param source
	 *            the identfier of the sender of the Ready message.
	 * @return <em>true</em> if the set does not contain the sender of the
	 *         message <em>false</em> in any other case.
	 */
	public boolean addReadyNode(Integer source) {
		return nodesSendReady.add(source);
	}

	/**
	 * @deprecated Checks if the sender a Ready message exists in the set of the
	 *             senders of Ready messages
	 * 
	 * @param source
	 *            the identfier of the sender of the Ready message.
	 * @return <em>true</em> if the set contained the sender of the message,
	 *         <em>false</em> in any other case.
	 */
	public boolean containsReadyNode(Integer source) {
		return nodesSendReady.contains(source);
	}

	/**
	 * Removes the sender of the Ready message from the set of the senders of
	 * Ready messages.
	 * 
	 * @param source
	 *            the identfier of the sender of the Ready message.
	 * @return <em>true</em> if the set contained the sender of the message,
	 *         <em>false</em> in any other case
	 */
	public boolean removeReadyNode(Integer source) {
		return nodesSendReady.remove(source);
	}

	/**
	 * Returns the string representation of the broadcast info in human readable
	 * form
	 */
	@Override
	public String toString() {
		String msg = "";
		msg += "Steps : " + step;
		msg += "\n#Echo Messages : " + numberOfEchoM;
		msg += "\n#Ready Messages : " + numberOfReadyM;
		msg += "\nNodes voted Echo :";
		for (Integer i : nodesSendEcho) {
			msg += " " + i;
		}
		msg += "\nNodes voted Ready :";
		for (Integer i : nodesSendReady) {
			msg += " " + i;
		}
		return msg;
	}
}
