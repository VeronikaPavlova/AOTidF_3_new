package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
import de.dailab.jiactng.agentcore.comm.message.JiacMessage;
import de.dailab.jiactng.agentcore.knowledge.IFact;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;
import de.dailab.jiactng.aot.auction.onto.Bid;
import de.dailab.jiactng.aot.auction.onto.CallForBids;
import de.dailab.jiactng.aot.auction.onto.CallForBids.CfBMode;
import de.dailab.jiactng.aot.auction.onto.EndAuction;
import de.dailab.jiactng.aot.auction.onto.InformBuy;
import de.dailab.jiactng.aot.auction.onto.InformBuy.BuyType;
import de.dailab.jiactng.aot.auction.onto.InformSell;
import de.dailab.jiactng.aot.auction.onto.InitializeBidder;
import de.dailab.jiactng.aot.auction.onto.Offer;
import de.dailab.jiactng.aot.auction.onto.Register;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.StartAuctions;
import de.dailab.jiactng.aot.auction.onto.Wallet;

/**
 * Very simple dummy implementation of bidder bean, for testing the
 * auctioneer and (maybe) as a starting point for students (or not).
 */
public class jonnybidder extends AbstractAgentBean {

	/*
	 * CONFIGURATION
	 */
	
	/** message group where to receive multicast messages from */
	private String messageGroup;
	
	/** the ID of this bidder; has to be unique */
	private String bidderId;
	
	/** used to identify agents belonging to the same group of students */
	private String groupToken;

	/** this agent will always bid a random fraction of its remaining credits */
	private Double biddingFraction;
	
	
	/** strategy of Bidder */
	private String bidderStrategy;

	
	private double offer = 0.0;
	private double rich = 7500.00;
	private double medium = 5000.00;
	private double poor = 2000.00;
	double cash;
	double myOffer = 0.00;
	
	/*
	 * STATE
	 */
	
	/** to identify and ignore duplicate calls for registration */
	private Set<Integer> seenAuctions = new HashSet<>();
	
	/** this bidder's wallet */
	private Wallet wallet;

	/** mapping auction types to auctioneer IDs */
	private Map<StartAuction.Mode, Integer> auctioneerIds;
	private Map<StartAuction.Mode, ICommunicationAddress> auctioneerAddresses;
	
	private Random random;
	
	/*
	 * LIFECYCLE METHODS
	 */
	
	@Override
	public void doStart() throws Exception {
		// join message group
		IGroupAddress group = CommunicationAddressFactory.createGroupAddress(messageGroup);
		IActionDescription joinAction = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
		invoke(joinAction, new Serializable[] {group});

		random = new Random(bidderId.hashCode());
		auctioneerIds = new HashMap<>();
		auctioneerAddresses = new HashMap<>();
		
		biddingFraction = 0.01 + 0.01 * Math.random();
		log.info(String.format("%s will bid fraction %.2f", bidderId, biddingFraction));
		
		// attach memory observer for handling messages
		memory.attach(new MessageObserver(), new JiacMessage());
	}

	@Override
	public void execute() {
		if (wallet == null) return;
		log.info(wallet);

		// randomly offer some of the currently owned items for sale
		StartAuction.Mode C = StartAuction.Mode.C;
		if (auctioneerIds.containsKey(C)) {
			List<Resource> whatToOffer = new ArrayList<>();
			do {
				Resource[] resources = Resource.values();
				Resource res = resources[random.nextInt(resources.length)];
				if (wallet.get(res) > 0) {
					whatToOffer.add(res);
				}
			} while (Math.random() > 0.3);
			if (! whatToOffer.isEmpty()) {
				send(new Offer(auctioneerIds.get(C), bidderId, whatToOffer, biddingFraction * wallet.getCredits()), auctioneerAddresses.get(C));
				// XXX TEST WITH FAULTY MESSAGES
//				send(new Offer(auctioneerIds.get(C), bidderId, whatToOffer, null), auctioneerAddresses.get(C));
			}
		}
	}
	
	/*
	 * MESSAGE HANDLING
	 */
	
	/**
	 * This memory observer will be triggered whenever a JIAC message arrives.
	 * We will then reply with an offer.
	 */
	@SuppressWarnings("serial")
	class MessageObserver implements SpaceObserver<IFact> {

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
					
					// send register
					send(new Register(bidderId, groupToken), message.getSender());
					// XXX TEST WITH FAULTY MESSAGES
//					send(new Register(bidderId, groupToken), message.getSender());
				}
				
				if (message.getPayload() instanceof InitializeBidder) {
			          InitializeBidder initBidder = (InitializeBidder) message.getPayload();
			          // get wallet, inspect items (optional)
			          if(bidderId.equalsIgnoreCase(initBidder.getWallet().getBidderId())) {
			            wallet = initBidder.getWallet();
			            log.info(wallet);
			          }else{
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
					CallForBids req = (CallForBids) message.getPayload();
					double offer = strategy(bidderStrategy,req);
					if(offer != -1) {
						send(new Bid(req.getAuctioneerId(), bidderId, req.getCallId(), offer), message.getSender());
					}
				}
				
				if (message.getPayload() instanceof InformBuy) {
					InformBuy inform = (InformBuy) message.getPayload();
					if (inform.getType() == BuyType.WON) {
						synchronized (wallet) {
							wallet.add(inform.getBundle());
							wallet.updateCredits(- inform.getPrice());
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
	
	/*
	 * HELPER METHODS
	 */
	
	private void send(IFact payload, ICommunicationAddress receiver) {
		log.info(String.format("Sending %s to %s", payload, receiver));
		JiacMessage msg = new JiacMessage(payload);
		IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		invoke(sendAction, new Serializable[] {msg, receiver});
	}
	
	
	private double strategy(String bidderStrategy, CallForBids req) {
		double bid = -1;
		cash = wallet.getCredits();
		if(cash < req.getMinOffer())
			return bid;
		if(bidderStrategy== "random") {

			if (Math.random() > 0.3) {
				if (req.getMode() == CfBMode.BUY) {
					bid = cash * biddingFraction;
					log.info(wallet);
				} else {
					if(!wallet.contains(req.getBundle()))
						return bid;
					bid = req.getMinOffer();
				}						
			}
			
		}else if (bidderStrategy=="jonny") {
			
			cash = wallet.getCredits();

				if (req.getMode() == CfBMode.BUY) {
						// determine offer and send unicast-reply to sender
						if(cash >= rich) {
							offer = cash * 0.10;
						}else if (cash < rich && cash >= medium) {
							offer = cash * 0.05;
						}else if (cash > 100 && cash <= poor){
							offer = cash * 0.01;
						}else {
							return bid;
						}
						bid = offer;
						
						
				}
				//SELL SELL SELL
				else {
					if(!wallet.contains(req.getBundle()))
						return bid;
					
						double min = req.getMinOffer();
						if(cash < min)
							return bid;
						
						if(cash >= rich) {
							myOffer = min * 0.01;
						}else if (cash < rich && cash >= medium) {
							myOffer = min * 0.05;
						}else {
							myOffer = min * 0.10;
						}
						bid = myOffer;
					}
				
		}
		else {
			if (req.getMode() == CfBMode.BUY) {
				bid = wallet.getCredits() * biddingFraction+ (0.1* wallet.getCredits());				
			}else {
				if(!wallet.contains(req.getBundle()))
					return bid;
				bid = req.getMinOffer();
			}
		}
		return bid;
	}

	/*
	 * GETTERS AND SETTERS
	 */

	public void setBidderStrategy(String bidderStrategy) {
		this.bidderStrategy = bidderStrategy;
	}
	
	public void setBidderId(String bidderId) {
		this.bidderId = bidderId;
	}
	
	public void setGroupToken(String groupToken) {
		this.groupToken = groupToken;
	}
	
	public void setMessageGroup(String messageGroup) {
		this.messageGroup = messageGroup;
	}

}

