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
public class BidderBeanCustom extends AbstractAgentBean {

	/*
	 * CONFIGURATION
	 */

	/** message group where to receive multicast messages from */
	private String messageGroup;

	/** the ID of this bidder; has to be unique */
	private String bidderId;

	/** used to identify agents belonging to the same group of students */
	private String groupToken;

	/** strategy of Bidder */
	private String bidderStrategy;

	/** this agent will always bid a random fraction of its remaining credits */
	private Double biddingFraction;
	
	private List <ArrayList> last10WBids;
	private List <ArrayList> last10Bids;
	private Double previousWinningBid;
	private Double initialWallet;
	private int prevCallId =-1;
	private double bidders =1;

	
	private int counterA =0;
	private int counterB =0;
	private int counterC =0;

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
		log.info(String.format("%s with strategy %s will bid fraction %.2f", bidderId, bidderStrategy, biddingFraction));
//		System.out.println(bidderId + " "+ bidderStrategy);

		// attach memory observer for handling messages
		memory.attach(new MessageObserver(), new JiacMessage());
	}

	@Override
	public void execute() {
		if (wallet == null) return;
//		System.out.println(bidderId + " "+ bidderStrategy);
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
//				log.info("Received " + message.getPayload());

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
						bidders=bidders + 1;
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
					System.out.println("counters from Bidder Bean");
					System.out.println(counterA );
					System.out.println(counterB);
					System.out.println(counterC );

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
//					System.out.println(wallet.toString());
//					String[] arrOfStr = wallet.toString().split("\\{"); 
//					String[] parts = wallet.toString().split("\\{"); 
//					String part2 = parts[1]; // 004
//					parts = part2.split("\\}"); 
//					part2 = parts[0]; 
//					String temp_S="";
//					for (String a : arrOfStr) 
//						temp_S += a; 
//					System.out.println(part2);

					double offer = strategy(bidderStrategy,req);
					if(offer != -1) {
						send(new Bid(req.getAuctioneerId(), bidderId, req.getCallId(), offer), message.getSender());
					}
//					if (Math.random() > 0.3) {
//						if (req.getMode() == CfBMode.BUY) {
//							// determine offer and send unicast-reply to sender
//							Double offer = wallet.getCredits() * biddingFraction;
//							send(new Bid(req.getAuctioneerId(), bidderId, req.getCallId(), offer), message.getSender());
//							// XXX TEST WITH FAULTY MESSAGES
////							send(new Bid(req.getAuctioneerId(), bidderId, req.getCallId(), null), message.getSender());
//						} else {
//							if (wallet.contains(req.getBundle())) {
//								send(new Bid(req.getAuctioneerId(), bidderId, req.getCallId(), req.getMinOffer()), message.getSender());
//								// XXX TEST WITH FAULTY MESSAGES
////								send(new Bid(req.getAuctioneerId(), bidderId, req.getCallId(), null), message.getSender());
//							}
//						}
//					}
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
//		log.info(String.format("Sending %s to %s", payload, receiver));
		JiacMessage msg = new JiacMessage(payload);
		IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		invoke(sendAction, new Serializable[] {msg, receiver});
	}
	private boolean isFound( CallForBids req, String Item) {
		return req.getBundle().toString().indexOf(Item) !=-1? true: false;
	}

	private double strategy(String bidderStrategy, CallForBids req) {
		double bid = -1;
		if(bidderStrategy.equalsIgnoreCase("random")) {
			if (Math.random() > 0.3) {
				if (req.getMode() == CfBMode.BUY) {
					bid = wallet.getCredits() * biddingFraction;
				} else {
					bid = req.getMinOffer();
				}
			}

		}else {
			if (req.getMode() == CfBMode.BUY && wallet.getValue() <= (-3000)) {
//				bid = wallet.getCredits() * biddingFraction+ (0.1* wallet.getCredits());

				if (req.getAuctioneerId()==1) {
					counterA++;
					if(!wallet.contains(req.getBundle())){
						bid = wallet.getValue() * ((bidders-1)/bidders);
//						boolean isFound = req.getBundle().toString().indexOf("F") !=-1? true: false;
//						if(isFound(req, "F") || isFound(req, "G")  || isFound(req, "E")) {
//							bid = (bid*0.2)+bid;
//						}
					}
//					req.getMinOffer()*((bidders-1)/bidders);
//					bid = wallet.getCredits() * 0.8;
				}else if (req.getAuctioneerId()==3){
//					System.out.println(req.getMinOffer()*((bidders-1)/bidders));
					counterC++;
					//bid = wallet.getCredits() * biddingFraction+ (0.1* wallet.getCredits());
					bid = req.getMinOffer() + (wallet.getValue()*((bidders-1)/bidders));
				}else {
				}
			}else {
				counterB++;
//				System.out.println(counterB + " " + req.getAuctioneerId());
				if(isFound(req, "A") || isFound(req, "B")) {
					bid = req.getMinOffer();
				}else if(isFound(req, "C") || isFound(req, "D")){
					bid = req.getMinOffer() + 0.2*req.getMinOffer();
				}
				else {
					bid = req.getMinOffer() + 0.4*req.getMinOffer();

				}
			}
		}
		return bid;
	}

	/*
	 * GETTERS AND SETTERS
	 */

	public void setBidderId(String bidderId) {
		this.bidderId = bidderId;
	}

	public void setGroupToken(String groupToken) {
		this.groupToken = groupToken;
	}

	public void setBidderStrategy(String bidderStrategy) {
		this.bidderStrategy = bidderStrategy;
	}

	public void setMessageGroup(String messageGroup) {
		this.messageGroup = messageGroup;
	}

}
