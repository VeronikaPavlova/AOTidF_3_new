package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import de.dailab.jiactng.aot.auction.onto.CallForBids;
import de.dailab.jiactng.aot.auction.onto.EndAuction;
import de.dailab.jiactng.aot.auction.onto.InformBuy;
import de.dailab.jiactng.aot.auction.onto.InformSell;
import de.dailab.jiactng.aot.auction.onto.InitializeBidder;
import de.dailab.jiactng.aot.auction.onto.Register;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.StartAuctions;
import de.dailab.jiactng.aot.auction.onto.Wallet;
import de.dailab.jiactng.aot.auction.onto.InformBuy.BuyType;

public class ExtremeBidderBean extends AbstractAgentBean {
		
	//Wallet
	private Wallet wallet;
		
	// Bidder ID
	private String bidderId;

	// Multicast message group
	private String messageGroup;
		
	//TODO
	private String groupToken;
		
	/** mapping auction types to auctioneer IDs */
	private Map<StartAuction.Mode, Integer> auctioneerIds;
	private Map<StartAuction.Mode, ICommunicationAddress> auctioneerAddresses;

	/** to identify and ignore duplicate calls for registration */
	private Set<Integer> seenAuctions = new HashSet<>();
		

	@Override
	public void doStart() throws Exception {
		
		System.out.println("Start Bidding Agent with ID " + bidderId);
		
		//Join Group
		Action joinGroup = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
		IGroupAddress group = CommunicationAddressFactory.createGroupAddress(messageGroup);
		this.invoke(joinGroup, new Serializable[]{group});
		auctioneerIds = new HashMap<>();
		auctioneerAddresses = new HashMap<>();
		
		
		//Listen to memory events
		this.memory.attach(new MessageObserver(), new JiacMessage());
		
	}
	
	@Override
	public void execute() {
		
		if (wallet == null) return;
		log.info(wallet);
	}
	
	private void send(IFact payload, ICommunicationAddress receiver) {
		log.info(String.format("Sending %s to %s", payload, receiver));
		JiacMessage msg = new JiacMessage(payload);
		IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		invoke(sendAction, new Serializable[] {msg, receiver});
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

	/**
	 * This memory observer will be triggered whenever a JIAC message arrives.
	 * We will then reply with an offer.
	 */
	@SuppressWarnings("serial")
	class MessageObserver implements SpaceObserver<IFact> {
		
		private void handleCallForBuy(CallForBids cfb, IJiacMessage message) {
			log.info("Bundle: " + cfb.getBundle());
			
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public void notify(SpaceEvent<? extends IFact> event) {
			// check the type of the event
			if (event instanceof WriteCallEvent) {
				// we know it's a message due to the template, but we have to check the content
				JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
				log.info("Received " + message.getPayload());
				
				// META AUCTIONEER
				
				if (message.getPayload() instanceof StartAuctions &&
						seenAuctions.add(((StartAuctions) message.getPayload()).getAuctionsId())) {
					
//					 send register
					send(new Register(bidderId, groupToken), message.getSender());
					// XXX TEST WITH FAULTY MESSAGES
//					send(new Register(bidderId, groupToken), message.getSender());
				}
				
				if (message.getPayload() instanceof InitializeBidder) {
					InitializeBidder initBidder = (InitializeBidder) message.getPayload();
					if(bidderId.equalsIgnoreCase(initBidder.getWallet().getBidderId())) {
						// get wallet, inspect items (optional)
						wallet = initBidder.getWallet();
					} else {
						return;
					}
					
				} 

				if (message.getPayload() instanceof EndAuction) {
					EndAuction endMsg = (EndAuction) message.getPayload();
					if (bidderId.equals(endMsg.getWinner())) {
						log.info("WE WON!  :-)");
					} else {
						log.info("WE LOST. :-(");
					}
					log.info("Winner's wallet: " + endMsg.getWallet());
					auctioneerIds.clear();
					auctioneerAddresses.clear();
					wallet = null;
				}

				// AUCTIONEERS A, B, C
				
				if (message.getPayload() instanceof StartAuction) {
					StartAuction startAuction = (StartAuction) message.getPayload();
					// remember auctioneer ID and address
					auctioneerIds.put(startAuction.getMode(), startAuction.getAuctioneerId());
					auctioneerAddresses.put(startAuction.getMode(), message.getSender());
				}
				
				if (message.getPayload() instanceof CallForBids) {
					CallForBids cfb = (CallForBids) message.getPayload();
				}
				
				if (message.getPayload() instanceof InformBuy) {
					InformBuy inform = (InformBuy) message.getPayload();
					if (inform.getType() == BuyType.WON) {
						synchronized (wallet) {
							wallet.add(inform.getBundle());
							wallet.updateCredits(- inform.getPrice());
							log.info("Nika won the bundle " + inform.getBundle());
						}
					}
				}
				
				if (message.getPayload() instanceof InformSell) {
					InformSell inform = (InformSell) message.getPayload();
					if (inform.getType() == InformSell.SellType.SOLD) {
						synchronized (wallet) {
							wallet.remove(inform.getBundle());
							wallet.updateCredits(inform.getPrice());
						}
					}
				}
								
				// once handled, the message should be removed from memory
				memory.remove(message);
			}
		}		
	}


	
}
