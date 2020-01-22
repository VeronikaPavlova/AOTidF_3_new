package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;

import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.agentcore.comm.message.IJiacMessage;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;
import de.dailab.jiactng.aot.auction.beans.BidderBean.MessageObserver;
import de.dailab.jiactng.aot.auction.onto.CallForBids;
import de.dailab.jiactng.aot.auction.onto.EndAuction;
import de.dailab.jiactng.aot.auction.onto.InformBuy;
import de.dailab.jiactng.aot.auction.onto.InformSell;
import de.dailab.jiactng.aot.auction.onto.InitializeBidder;
import de.dailab.jiactng.aot.auction.onto.Register;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.StartAuctions;
import de.dailab.jiactng.aot.auction.onto.Wallet;

/**
 * TODO Implement this class.
 * 
 * You might also decide to split the logic of your bidder up onto several
 * agent beans, e.g. one for each type of auction. In this case, remember
 * to keep the agent's `Wallet` in synch between the different roles, e.g.
 * using the agent's memory, as seen for the auctioneer beans.
 */
public class BidderBeanStub extends AbstractAgentBean {

	//Wallet
	private Wallet wallet;
	
	// Bidder ID
	private String bidderId;

	// Multicast message group
	private String messageGroup;
	
	//TODO
	private String groupToken;
	
	
	@Override
	public void doStart() throws Exception {
		
		System.out.println("Start Bidding Agent with ID " + bidderId);
		System.out.println("GT: " + groupToken);
		System.out.println("MG: " + messageGroup);
		
		//Join Group
		Action joinGroup = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
		IGroupAddress group = CommunicationAddressFactory.createGroupAddress(messageGroup);
		this.invoke(joinGroup, new Serializable[]{group});
		
		//Listen to memory events
		this.memory.attach(new MessageObserver(), new JiacMessage());
		
	}
	
	@Override
	public void execute() {

	}
	
	
	public void setMessageGroup(String messageGroup) {
		this.messageGroup = messageGroup;
	}
	
	public String getMessageGroup() {
		return messageGroup;
	}

	public void setbidderId(String bidderId) {
		this.bidderId = bidderId;
	}
	
	public String getbidderId() {
		return bidderId;
	}
	
	public void setgroupToken(String groupToken) {
		this.groupToken = groupToken;
	}
	
	public String getgroupToken() {
		return groupToken;
	}

	/*
	 * TODO
	 * when the agent starts, create a message observer and attach it to the
	 * agent's memory. that message observer should then handle the different
	 * messages and send a suitable Bid in reply. see the readme and the
	 * sequence diagram for the expected order of messages.
	 */
	
	private class MessageObserver implements SpaceObserver<IFact>{

		/**
		 * Listener - notified each time something is read, written to, removed from, or updated in the memory
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void notify(SpaceEvent<? extends IFact> event) {
			if (event instanceof WriteCallEvent) {
				
				WriteCallEvent<IJiacMessage> wce = (WriteCallEvent<IJiacMessage>) event;
								
				//consume message
				IJiacMessage message = memory.remove(wce.getObject());

				//What Type of message received?
				IFact payload = message.getPayload();
				log.info("Received message " + payload);
				

				//Now do different actions depending on the received message from the Auctioneer
				if(payload instanceof StartAuctions) {
					System.out.println("Register to Meta Auctioneer");
					register(message.getSender());
				} else if (payload instanceof InitializeBidder) {
					/**
					 * meta auctioneer sends this message including the initial wallet to the 
					 * bidder and then starts the different auctions for types A,B,C
					 * 
					 */
					getInitializeBidder();
				} else if (payload instanceof StartAuction) {
					/*
					 * TODO You will receive your initial "Wallet" from the auctioneer, but
					 * afterwards you will have to keep track of your spendings and acquisitions
					 * yourself. The Auctioneer will do so, as well.
					 */
					
					/**
					 * we get the items and the mode of the Auction (fixed item or random)
					 * 
					 * don't need to reply but should use the message to get the auctioneers address and auctioneers ID 
					 * (needed for sending bids and offers)
					 * 
					 * Also if the Auction is C we could after receiving this message send an Offer()
					 * 
					 * have here to update our Wallet
					 */
					getStartAuction();
				} else if (payload instanceof CallForBids) {
					/**
					 * If it comes from 
					 * 	Auction A: Bidding for buying,
					 * 	Auction B: Bidding for Selling
					 * 	Auction C: Offering and Bidding
					 * 
					 * Should in this case differentiate which Auction is sending us the message and
					 * depending what should we do --> need a strategy
					 */
					bid();
				} else if (payload instanceof InformBuy) {
					/**
					 * Inform whether the item was bought or not (however not information by whom or at which price)
					 * 
					 * send to all bidders
					 */
					getInformBuy();
				} else if (payload instanceof InformSell) {
					/**
					 * Inform that item was sold and at which price
					 * 
					 * send to all bidders
					 */
					getInformSell();
				} else if (payload instanceof EndAuction) {
					/**
					 * At the end the Meta Auctioneer will send message who is the winner of the auction and
					 * their final Wallet
					 * 
					 */
					log.info("Current wallet of the Bidder " + bidderId + " :" + wallet);
				}				
			}
			
		}

		private void getInformSell() {
			// TODO Auto-generated method stub
			
		}

		private void getInformBuy() {
			// TODO Auto-generated method stub
			
		}

		private void bid() {
			// TODO Auto-generated method stub
			
		}

		private void getStartAuction() {
			offer();
			// TODO Auto-generated method stub
			
		}

		private void offer() {
			// TODO Auto-generated method stub
			
		}

		private void getInitializeBidder() {
			// TODO Auto-generated method stub
			
		}

		/**
		 * Bidder sends Register message to the auctioneer, including it's own bidder ID and unique group token
		 * @param sender
		 */
		private void register(ICommunicationAddress sender) {
			Register register = new Register(bidderId, groupToken);
			
			JiacMessage msg = new JiacMessage(register);
			
			IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
			invoke(sendAction,new Serializable[] {msg, sender});	
		}	
	}	
}
