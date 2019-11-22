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

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.broadcast.IBroadcast;
import gr.uoa.di.dsg.communicator.AbstractCommunicator;
import gr.uoa.di.dsg.communicator.Message;
import gr.uoa.di.dsg.communicator.Node;
import gr.uoa.di.dsg.ic.ApplicationGetter;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Panos
 * @version 0.0.1 The Java implementation of Bracha's Reliable broadcast
 *          primitive (See <a href="http://bit.ly/10QhrYi"> Gabriel Bracha</a>).
 *          It also implements the IBroadcast Interface (@link
 *          gr.uoa.di.broadcast.IBroadcast). The broadcast progresses in steps.
 *          <p>
 *          The outline of the broadcast primitive is roughly as follows:
 *          <ul>
 *          <li>During the first step a node invokes the broadcast function to
 *          send a message to all other nodes.</li>
 *          <li>During the second step each node that received an initial
 *          message or has gathered a sufficient number of <em>Echo</em> or
 *          <em>Ready</em> messages it echoes the value that those messages hold
 *          to all recipients through an <i>Echo Message</i></li>
 *          <li>
 *          During the third step each node that has gathered a sufficient
 *          number of <em>Echo</em> or <em>Ready</em> messages it sends the
 *          value that those messages hold to all recipients through a <i>Ready
 *          Message</i></li>
 *          <li>
 *          After gathering a sufficient number of <em>Ready</em> messages it
 *          concludes the broadcast</li>
 *          </ul>
 *          This protocol deviates from the actual Bracha's protocol since it is
 *          designed to have a handler for each type of message. Since the
 *          protocol allows nodes to initiate broadcasts with the same id but
 *          regarding different values, the implementation stores all different
 *          values that receives through messages. This means that we can have
 *          two different values corresponding to the same ID. However, due to
 *          the protocol's structure only one of those broadcasts will be
 *          eventually completed while all others will never terminate.
 */
public class BrachaBroadcast implements IBroadcast
{
	/**
	 * The total number of nodes in the system
	 */
	private int numNodes = 0;
	
	/**
	 * The number of total tolerated faults. This is calculated by a simple formula: (numNodes - 1)/3
	 */
	private int toleratedFaults = 0;

	/**
	 * The communicator instance
	 */
	private transient AbstractCommunicator communicator = null;

	/**
	 * The applicationGetter instance
	 */
	private transient ApplicationGetter applicationGetter = null;
	
	/**
	 * A map that stores all the active consensus where messages pending to be 
	 * delivered.
	 */
	private Map<String, HashMap<String,BroadcastInfo> > activeBroadcasts = null;
	
	/**
	 * The set of broadcast that have been completed. This structure is used for
	 * ignoring messages destined for already completed broadcasts.
	 */
	private Set<String> completedBroadcasts = null;
	
	/**
	 * The communication group that this broadcast module belongs.
	 */
	private String nodeGroup = null;

	/**
	 * Constructor for proper initialization
	 */
	public BrachaBroadcast() {}
	
	/**
	 * The initialize function of the IBroadcast interface. It is used for
	 * registering a series of message handlers to the communicator for the
	 * deserialization and handling of messages.
	 */
	@Override
	public void initialize(AbstractCommunicator comm, ApplicationGetter appCr, String nodeGroup, int numNodes)
	{
		comm.registerMessage(BBMessageType.INIT.getValue(), (byte[] data) -> InitMessage.deserialize(data) , (Message msg, Node source) -> OnInitMessage((InitMessage)msg, source));
		comm.registerMessage(BBMessageType.ECHO.getValue(), (byte[] data) -> EchoMessage.deserialize(data) , (Message msg, Node source) -> OnEchoMessage((EchoMessage)msg, source));
		comm.registerMessage(BBMessageType.READY.getValue(), (byte[] data) -> ReadyMessage.deserialize(data) , (Message msg, Node source) -> OnReadyMessage((ReadyMessage)msg, source));
		
		this.communicator = comm;
		this.applicationGetter = appCr;
		this.nodeGroup = nodeGroup;
		this.numNodes = numNodes;
		
		this.toleratedFaults = (this.numNodes - 1)/3;
		this.activeBroadcasts = new HashMap<>();
		this.completedBroadcasts = new HashSet<>();
	}
	
	/**
	 * The broadcast function of the IBroadcast interface. It is used for the
	 * dissemination of values. It constructs a unique ID that describes the
	 * broadcast by appending the different ids using the ":" as delimiter (e.g
	 * "icid:cid:pid:bid" )
	 */
	@Override
	public void broadcast(String icid, int cid, int pid, int bid, String value)
	{	 
		//To check if a message is for a broadcast that we are not participating!
		String UUID = icid + ":" + cid + ":" + pid + ":" + bid;
		addedNowOnBroadcasts(UUID, value, pid);
		
		InitMessage msg = new InitMessage(icid, cid, pid, bid, value);
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BrachaBroadcast, Node: " + pid + "]: Starting a broadcast for " + msg.toString());
		
		//Actual Broadcast code!
		this.communicator.sendGroup(nodeGroup, msg);
		this.activeBroadcasts.get(UUID).get(value).setStep(1);
	}

	/**
	 * The handler for the Init messages of the broadcast protocol. In case the
	 * node is at step 1 it sends an <em>Echo</em> message to all nodes and
	 * progress the node to step 2. In any other case it just ignores the
	 * message.
	 * 
	 * @param msg
	 *            The actual InitMessage to be processes.
	 * @param source
	 *            The sender of that message.
	 */
	public void OnInitMessage(InitMessage msg, Node source)
	{
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BrachaBroadcast, Node: " + communicator.getCurrentNode().getNodeId() + "]: An InitMessage was received: " + msg.toString() + " from Node " + source.getNodeId());
		
		//check if the broadcast is completed
		if(this.completedBroadcasts.contains(msg.getUUID())) {
			if(GlobalVariables.HIGH_VERBOSE)
				System.out.println("[BrachaBroadcast, Node: " + communicator.getCurrentNode().getNodeId() + "]: IGNORE_1 " + msg.toString() + " from Node " + source.getNodeId());
			
			return;
		}
		
		//to check if a message is for a broadcast that we are not participating!
		if(addedNowOnBroadcasts(msg.getUUID(), msg.getValue(),msg.getNodeID()))
			this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).setStep(1);
		
		//Actual Broadcast code
		if(this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).getStep() ==  1)
		{
			this.communicator.sendGroup(nodeGroup, new EchoMessage(msg.getApplicationID(), msg.getConsensusID(), msg.getNodeID() , msg.getBroadcastID(), msg.getValue()));
			this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).setStep(2);
			//this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).addNode(source.getProcessID());
		}
		else {
			if(GlobalVariables.HIGH_VERBOSE)
				System.out.println("[BrachaBroadcast, Node: " + communicator.getCurrentNode().getNodeId() + "]: IGNORE_2 " + msg.toString() + " from Node " + source.getNodeId()
					+ ", at STEP: " + this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).getStep());
		}
	}
	
	/**
	 * The hanlder of the Echo message of the broadcast protocol. In case the
	 * node is at step 1 and has gathered a sufficient number of <em>Echo</em>
	 * messages it sends an <em>Echo</em> message to all nodes. In case the node
	 * is at step 2 and has gathered a sufficient number of
	 * <em>Echo</em> messages it sends a <em>Ready</em> message to all other
	 * nodes. In any other case it just ignores the message
	 * 
	 * @param msg
	 *            The actual Echo message to be processed.
	 * @param source
	 *            The sender of the message.
	 */
	public void OnEchoMessage(EchoMessage msg, Node source)
	{
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BrachaBroadcast, Node: " + communicator.getCurrentNode().getNodeId() + "]: An EchoMessage was received: " + msg.toString() + " from Node " + source.getNodeId());
		
		//check if the broadcast is completed
		if(this.completedBroadcasts.contains(msg.getUUID()))
			return;
		
		//to check if a message is for a broadcast that we are not participating!
		if(addedNowOnBroadcasts(msg.getUUID(), msg.getValue(),msg.getNodeID()))
			this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).setStep(1);
	
		
		//To update the structures that this broadcast for all values that this node supports
		if( anyOtherPhaseCompleted(msg.getUUID(), msg.getValue(),2) == false)
		{
			this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).addEchoNode(source.getNodeId());
			this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).increaseNumberOfEchoM();
			updateOtherStructures(msg.getUUID(), msg.getValue(), source.getNodeId(),BBMessageType.ECHO);
		}
		
		//Actual Broadcast code
		int numberOfEchoM = this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).getNumberOfEchoM();
		int numberOfSteps = this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).getStep();
		if(numberOfEchoM == Math.ceil((this.numNodes + this.toleratedFaults)/2))
		{
			if(numberOfSteps == 1)
			{
				this.communicator.sendGroup(nodeGroup, new EchoMessage(msg.getApplicationID(), msg.getConsensusID(), msg.getNodeID() , msg.getBroadcastID(), msg.getValue()));
				this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).setStep(2);
			}
			else if(numberOfSteps == 2)
			{
				this.communicator.sendGroup(nodeGroup, new ReadyMessage(msg.getApplicationID(), msg.getConsensusID(), msg.getNodeID() , msg.getBroadcastID(), msg.getValue()));
				this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).setStep(3);
			}
		}
	}

	/**
	 * The handler of Ready message of the broadcast protocol. In case a node is
	 * at step 1 and has gathered a sufficient number of <em>Ready</em> messages
	 * it sends an <em>Echo</em> message to all nodes. In case the node is at
	 * step 2 and has gathered a sufficient number of <em>Ready</em> messages it
	 * sends a <em>Ready</em> message to all other nodes. Finally, it the node
	 * is at step 3 and has gathered a sufficient number of
	 * <em>Ready</em> messages it performs the callback to the
	 * application through the use of the {@link gr.di.uoa.dsg.ic.ApplicationGetter}.
	 * 
	 * @param msg
	 *            The actual Ready message to be processed
	 * @param source
	 *            The sender of the message.
	 */
	public void OnReadyMessage(ReadyMessage msg, Node source)
	{
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BrachaBroadcast, Node: " + communicator.getCurrentNode().getNodeId() + "]: A ReadyMessage was received: " + msg.toString() + " from Node " + source.getNodeId());
		
		//check if the broadcast is completed
		if(this.completedBroadcasts.contains(msg.getUUID()))
		{
			return;
		}
		
		//to check if a message is for a broadcast that we are not participating!
		if(addedNowOnBroadcasts(msg.getUUID(), msg.getValue(),msg.getNodeID()))
			this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).setStep(1);
		
		//To update the structures that this broadcast for all values that this node supports
		this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).addReadyNode(source.getNodeId());
		this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).increaseNumberOfReadyM();
		updateOtherStructures(msg.getUUID(), msg.getValue(), source.getNodeId(),BBMessageType.READY);
		
		//Actual Broadcast code
		int numberOfReadyM = this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).getNumberOfReadyM();
		int numberOfSteps = this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).getStep();
	
		if(numberOfReadyM == this.toleratedFaults + 1)
		{
			if(numberOfSteps == 1)
			{
				this.communicator.sendGroup(nodeGroup, new EchoMessage(msg.getApplicationID(), msg.getConsensusID(), msg.getNodeID(), msg.getBroadcastID(), msg.getValue()));
				this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).setStep(2);
			}
			else if( numberOfSteps == 2)
			{
				this.communicator.sendGroup(nodeGroup, new ReadyMessage(msg.getApplicationID(), msg.getConsensusID(), msg.getNodeID(), msg.getBroadcastID(), msg.getValue()));
				this.activeBroadcasts.get(msg.getUUID()).get(msg.getValue()).setStep(3);
			}
		}
		else if( numberOfReadyM == 2 * this.toleratedFaults +1)
		{
			if(numberOfSteps ==  3)
			{
				this.completedBroadcasts.add(msg.getUUID());
				this.activeBroadcasts.remove(msg.getUUID());
				BroadcastAccept acc = new BroadcastAccept(msg.getApplicationID(), msg.getConsensusID(), msg.getNodeID() , msg.getBroadcastID(), msg.getValue());
				
				if(GlobalVariables.HIGH_VERBOSE)
					System.out.println("[BrachaBroadcast, Node: " + communicator.getCurrentNode().getNodeId() + "]: Accepting message " + acc.toString());
				
				applicationGetter.getApp(acc.getApplicationID()).process(acc);
			}
		}
	}
	
	/**
	 * This function is used to add active broadcast to the appropriate
	 * structure. It utilizes a Map strucure that map a UUID with different
	 * values.
	 * 
	 * @param broadcastUUID
	 *            The id that uniquely describes a broadcast
	 * @param value
	 *            The actual value that corresponds to the broadcastUUID
	 * @param processID
	 *            The initial sender of the message, since a message can be
	 *            echoed by another node.
	 * @return <em>true</em> if the tuple <broadcastUUID, value> did not existed
	 *         in the the structure, <em>false</em> in any other case.
	 */
	private boolean addedNowOnBroadcasts(String broadcastUUID, String value, int processID)
	{
		if(!this.activeBroadcasts.containsKey(broadcastUUID))
		{
			this.activeBroadcasts.put(broadcastUUID, new HashMap<>());
			this.activeBroadcasts.get(broadcastUUID).put(value, new BroadcastInfo(processID));
			return true;
		}
		else if(!this.activeBroadcasts.get(broadcastUUID).containsKey(value))
		{
			this.activeBroadcasts.get(broadcastUUID).put(value, new BroadcastInfo(processID));
			return true;
		}
		return false;
	}
	
	/**
	 * This function is used to check if a step regarding a value of a specific
	 * uuid has been completed. In these cases we need to disregard the message
	 * destined for that step.
	 * 
	 * @param uuid
	 *            The id that uniquely describes the broadcast
	 * @param value
	 *            The value that maps to the uuid
	 * @param greaterThanStep
	 *            The step that the broadcast regarding that value is.
	 * @return <em>true</em> if the phase regarding the tuple <uuid, value> has
	 *         been completed, <em>false</em> in any other case
	 */
	private boolean anyOtherPhaseCompleted(String uuid, String value, int greaterThanStep)
	{	
		for (String valueAsKey : this.activeBroadcasts.get(uuid).keySet()) 
		{
			if(this.activeBroadcasts.get(uuid).get(valueAsKey).getStep() > greaterThanStep)
				return true;
		}
		return false;
	}
	
	/**
	 * This function is used to update the number of echo and ready messages
	 * regarding a value of a specific broadcast id.
	 * 
	 * @param uuid
	 *            The id that uniquely describes the broadcast
	 * @param value
	 *            The value that maps to the uuid
	 * @param processID
	 *            The initial sender of the message, since a message can be
	 *            echoed by another node.
	 * @param step
	 *            The type of the message a.k.a Init, Echo, Ready.
	 * @return <em>true</em> in case the update was successful, <em>false</em>
	 *         in any other case.
	 */
	private boolean updateOtherStructures(String uuid, String value, Integer processID, BBMessageType step)
	{	
		for (String valueAsKey : this.activeBroadcasts.get(uuid).keySet()) 
		{
			if( !valueAsKey.equals(value) )
		    {
				switch(step){
					case ECHO:
							if(this.activeBroadcasts.get(uuid).get(valueAsKey).removeEchoNode(processID)){
								this.activeBroadcasts.get(uuid).get(valueAsKey).descreaseNumberOfEchoM();
								return true;
							}
						break;
					case READY:
							if(this.activeBroadcasts.get(uuid).get(valueAsKey).removeReadyNode(processID)){
								this.activeBroadcasts.get(uuid).get(valueAsKey).descreaseNumberOfReadyM();
								return true;
							}
						break;
					default:
						break;
				}
		    }
		}
		return false;
	}

	@Override
	public AbstractCommunicator getCommunicator() {
		return this.communicator;
	}
}
