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
package gr.uoa.di.dsg.consensus.bracha;

import gr.uoa.di.dsg.broadcast.BroadcastAccept;
import gr.uoa.di.dsg.ic.Application;
import gr.uoa.di.dsg.utils.GlobalVariables;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BBConsensus {

	Application parent = null;
	//Validation de-module
	protected Map<Integer, List<BroadcastAccept>> nonValidatedMessages = null;
	protected Map<Integer, Map<String,List<BroadcastAccept>>> validatedMessages = null;

	String icid;
	int cid;
	int pid;
	int phase = -1;
	int round = -1;
	int numNodes = 0;
	int toleratedFaults = 0;
	String value = null;

	//Validation procedures
	public void attemptToValidatePreviousMessages() {
		
		if(this.nonValidatedMessages.isEmpty())
			return;
		
		for(Integer key : this.nonValidatedMessages.keySet()){
			Iterator<BroadcastAccept> itr = this.nonValidatedMessages.get(key).iterator();
			while(itr.hasNext()){
				BroadcastAccept acc = itr.next();
				if(validateProcedure(acc)){
					//System.out.println("Entering here!"+acc.toString());
					this.addToValidatedMessages(acc);
					itr.remove();
				}
			}
		}
	}
	
	public void validateMessage(BroadcastAccept acc, int phase, int round) {
			
			int mRound = acc.getBroadcastID()%3;
			if(validateProcedure(acc)){
				this.addToValidatedMessages(acc);
			}else{
				this.addToNonValidatedMessages(acc);
			}
			iterativeRevalidationProc(mRound, round);
	}
	
	private void iterativeRevalidationProc(int messageRound, int currentRound) {
		
		for(int i = messageRound+1; i <= currentRound; i++) {
			
			if(!this.nonValidatedMessages.containsKey(i))
				continue;
			
			Iterator<BroadcastAccept> itr = this.nonValidatedMessages.get(i).iterator();
			while(itr.hasNext()){
				BroadcastAccept acc = itr.next();
				if(validateProcedure(acc)){
					this.addToValidatedMessages(acc);
					itr.remove();
				}
			}
		}
	}
	
	public boolean validateProcedure(BroadcastAccept acc) {
		int mRound = acc.getBroadcastID()%3;
	
		if(mRound ==  0)
			return true;
		return protocolFunctionN(acc, mRound);
	}
	
	public boolean protocolFunctionN(BroadcastAccept acc, int messageRound)
	{
		//If previous bid does not exists return false
		if(!this.validatedMessages.containsKey(acc.getBroadcastID()-1))
			return false;
		
		if(!this.validatedMessages.get(acc.getBroadcastID()-1).containsKey(BBConsensus.getActualValue(acc.getValue(),"(d,",")")))
			return false;
		
		int cardinalityValue = getCardinality(acc.getBroadcastID()-1, BBConsensus.getActualValue(acc.getValue(),"(d,",")"));
		if(messageRound == 1){
			if(cardinalityValue >= Math.ceil( (this.numNodes - this.toleratedFaults) / 2.0) )
					return true;
		}
		else if(messageRound == 2){
			if(acc.getValue().contains("(d,")){
				if( cardinalityValue >= Math.floor(this.numNodes / 2.0) + 1)
					return true;
			}
			else if(cardinalityValue >= 1){
				return true;
			}
		}
		return false;
	}
	
	private int getCardinality(int round, String value) {
		return this.validatedMessages.get(round).get(value).size();
	}

	public Map<Integer, Map<String,List<BroadcastAccept>>> getValidatedMessage() {
		return this.validatedMessages;
	}
	
	private void addToNonValidatedMessages(BroadcastAccept acc) {
		if(!nonValidatedMessages.containsKey(acc.getBroadcastID()))
			this.nonValidatedMessages.put(acc.getBroadcastID(), new ArrayList<>());
		this.nonValidatedMessages.get(acc.getBroadcastID()).add(acc);
	}

	private void addToValidatedMessages(BroadcastAccept acc) {
		
		if(!validatedMessages.containsKey(acc.getBroadcastID())){
			this.validatedMessages.put(acc.getBroadcastID(), new HashMap<>());
			this.validatedMessages.get(acc.getBroadcastID()).put(acc.getValue(), new ArrayList<>());
		}
		else if(!this.validatedMessages.get(acc.getBroadcastID()).containsKey(acc.getValue())){
			this.validatedMessages.get(acc.getBroadcastID()).put(acc.getValue(), new ArrayList<>());
		}
		this.validatedMessages.get(acc.getBroadcastID()).get(acc.getValue()).add(acc);
	}
	
	//actual consensus algorithm
	public BBConsensus(Application p, String appID, int nodeID, int cid, int numNodes) {
		this.parent = p;
		this.cid = cid;
		this.icid = appID;
		this.pid =  nodeID;
		this.numNodes = numNodes;
		this.toleratedFaults = (this.numNodes - 1) / 3;
		nonValidatedMessages = new HashMap<>();
		validatedMessages = new HashMap<>();
	}

	public void start(String value) {
		this.value = value;
		this.phase = 0;
		this.round = 0;
		int bid = 3 * this.phase + this.round;
		
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBConsensus, Node: " + pid + "]: Starting a broadcast for <" + icid + ", " + cid + ", " + bid + ", " + value + ">");

		this.parent.getBroadcast().broadcast(this.icid, this.cid, this.pid, bid, value);
		this.attemptToValidatePreviousMessages();
	}
	
	private int getTotalValidatedMessages(int bid, String value) {
		List<BroadcastAccept> sizeList = this.getValidatedMessage().get(bid).get(value);
		
		if(sizeList == null)
			return 0;
		return sizeList.size();
	}

	public void process(BroadcastAccept acc) {
		if(GlobalVariables.HIGH_VERBOSE)
			System.out.println("[BBConsensus, Node: " + pid + "]: A BroadcastAccept message was received: " + acc.toString());
		
		//This means that we have received a message from a previous phase
		if(phase>0 && acc.getBroadcastID() < 3 * this.phase )
		{
			if(GlobalVariables.HIGH_VERBOSE)
				System.out.println("The consensus has advanced to another phase. Ignoring message "+acc.toString());
			
			return;
		}
		this.attemptToValidatePreviousMessages();
//		System.out.println("ICID: "+this.icid+" CID: "+this.cid + " Phase & Round: "+this.phase+" "+this.round+
//						   " NON Valid:"+Arrays.asList(this.validationModule.nonValidatedMessages).toString()+
//						   " Valid :"+Arrays.asList(this.validationModule.validatedMessages).toString()+
//						   " ValNumNodes :"+this.validationModule.numNodes);
		
		// try to validate message
		this.validateMessage(acc, this.phase, this.round);
		boolean enoughValidatedMessages = hasNminusTMessages();
		
//		if(validatedValue == null){
//			System.out.println("ICID: "+this.icid+" CID: "+this.cid + " Phase & Round: "+this.phase+" "+this.round+
//			   " NON Valid:"+Arrays.asList(this.validationModule.nonValidatedMessages).toString()+
//			   " Valid :"+Arrays.asList(this.validationModule.validatedMessages).toString()+
//			   " ValNumNodes :"+this.validationModule.numNodes);
//		}
		
		if ( enoughValidatedMessages ) {
			if (this.round == 0) {
				String newValue =  getMajorityValue();
				int bid = 3 * this.phase + this.round;
				
				if(!this.value.equals(newValue)) {
					if(getTotalValidatedMessages(bid, newValue) > getTotalValidatedMessages(bid, value)) {
						this.value = newValue;
					}
				}
				
				this.round++;
				this.parent.getBroadcast().broadcast(this.icid, this.cid, this.pid, 3 * this.phase + this.round, value);
			}
			else if (this.round == 1) {
				String newValue =  getMajorityValue();
				if(getTotalValidatedMessages(3* this.phase + this.round, newValue) >= Math.floor(this.numNodes/2.0) + 1 )
					this.value="(d,"+newValue+")";
				
				this.round++;
				this.parent.getBroadcast().broadcast(this.icid, this.cid, this.pid, 3* this.phase + this.round, value);
			}
			else if(this.round == 2){
				String newValue =  getMajorityValue();
				int totalMessages = getTotalValidatedMessages(3* this.phase + this.round, newValue);
				
				//System.out.println(this.cid+"        "+newValue+"        "+totalMessages);
				if(newValue.contains("(d,")) {
					//call the upper level since consensus concluded
					if( totalMessages >= 2*this.toleratedFaults + 1){
						this.value = BBConsensus.getActualValue(newValue,"(d,",")");
						
						if(GlobalVariables.HIGH_VERBOSE)
							System.out.println("[BBConsensus, Node: " + pid + "]: In round 2, with value " + value + ", the Consensus is terminating...");
						
						//this.phase = 0;
						//this.round = 0;
						this.parent.processConsensusResult(this.cid, this.value);
						
					}
					//go to next phase with new value
					else if(totalMessages >= this.toleratedFaults + 1) {
						this.value = BBConsensus.getActualValue(newValue,"(d,",")");
						cleanUpForNewPhase();
						this.parent.getBroadcast().broadcast(this.icid, this.cid, this.pid, 3* this.phase +this.round, value);
					}
				}
				else{
					this.value = coin_toss();
					cleanUpForNewPhase();
					this.parent.getBroadcast().broadcast(this.icid, this.cid, this.pid, 3* this.phase +this.round, value);
				}
			}
			else{
				throw new RuntimeException("There is no 4th round in the Consensus protocol");
			}
		}
	}

	private String coin_toss(){
		SecureRandom random = new SecureRandom();
		return ""+random.nextInt(2)+"";
	}
	
	private void cleanUpForNewPhase() {
		this.phase++;
		this.round = 0;
		nonValidatedMessages.clear();
		validatedMessages.clear();
	}

	public static String getActualValue(String newValue, String pattern1, String pattern2) {
			Pattern p = Pattern.compile(Pattern.quote("(d,") + "(.*?)" + Pattern.quote(")"));
			Matcher m = p.matcher(newValue);
			if(newValue.contains(pattern1) && newValue.contains(pattern2)) {
				m.find();
				return m.group(1);
			}
			return newValue;
	}

	private boolean hasNminusTMessages() {
		int bid =  3 * this.phase + this.round;
	
		if(!this.getValidatedMessage().containsKey(bid)){
			return false;
		}
		
		int size = 0;
		if(!this.getValidatedMessage().get(bid).isEmpty()){
			for(String value : this.getValidatedMessage().get(bid).keySet()) {
				size += this.getValidatedMessage().get(bid).get(value).size();
			}
		}
		
		if(size >= this.numNodes - this.toleratedFaults)
			return true;
		return false;
	}

	private String getMajorityValue() {
		int bid =  3 * this.phase + this.round;
		
		int sizeMax = -1;
		String maxValue = null;
		
		for(String value : this.getValidatedMessage().get(bid).keySet()) {
			if(this.getValidatedMessage().get(bid).get(value).size() > sizeMax){
				sizeMax = this.getValidatedMessage().get(bid).get(value).size();
				maxValue = value;
			}
		}
		
		return maxValue;
	}
}
