package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;
import java.util.*;

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
import de.dailab.jiactng.aot.auction.onto.Bid;
import de.dailab.jiactng.aot.auction.onto.CallForBids;
import de.dailab.jiactng.aot.auction.onto.EndAuction;
import de.dailab.jiactng.aot.auction.onto.InformBuy;
import de.dailab.jiactng.aot.auction.onto.InformSell;
import de.dailab.jiactng.aot.auction.onto.InitializeBidder;
import de.dailab.jiactng.aot.auction.onto.Item;
import de.dailab.jiactng.aot.auction.onto.Register;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.StartAuction.Mode;
import de.dailab.jiactng.aot.auction.onto.StartAuctions;
import de.dailab.jiactng.aot.auction.onto.Wallet;
import de.dailab.jiactng.aot.auction.onto.CallForBids.CfBMode;
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
	
    private Dictionary<String, Double> minB = new Hashtable<String, Double>(); 
    private Dictionary<String, Double> maxB = new Hashtable<String, Double>(); 
    private int biddersN = 1;
    
    /** To store the number of times an element appear in a bundle*/
	private Map<String,Integer> numberOfItems = new HashMap<String,Integer>();
		

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
		
		@SuppressWarnings("unused")
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
						// get wallet, inspect items, init min,max for B Auctions
						wallet = initBidder.getWallet();
				        initalizeMinMaxOnB(minB,maxB);
					} else {
						biddersN = biddersN +1;
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
					// random code for testing
//					CallForBids cfb = (CallForBids) message.getPayload();
//					if(cfb.getAuctioneerId() ==2) {
//						System.out.println("MinB " + getMinB(cfb.getBundle()));
//						System.out.println("MaxB " + getMaxB(cfb.getBundle()));
//					}
//					tempRandomStrategy(cfb,wallet,message);
				}
				
				if (message.getPayload() instanceof InformBuy) {
					InformBuy inform = (InformBuy) message.getPayload();
					if (inform.getType() == BuyType.WON) {
						synchronized (wallet) {
							wallet.add(inform.getBundle());
							wallet.updateCredits(- inform.getPrice());
							log.info("Group5 won the bundle " + inform.getBundle());
						}
					}
				}
				
				if (message.getPayload() instanceof InformSell) {
					InformSell inform = (InformSell) message.getPayload();
					updateMinMaxOnB(minB, maxB,inform.getBundle(),inform.getPrice());
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
	
	//This will Return a Map with the Number of times each letter appear on the bundle	
	private int[] bundleItemsCounter(List<Resource> bundle) 
	{
		Integer A=0, B=0, C=0, D=0, E=0, F=0, G=0;
		
		for(Resource res: bundle) 
		{	
			switch (res) 
			{
				case A: A++; break;
				case B: B++; break;
				case C: C++; break;
				case D: D++; break;
				case E: E++; break;
				case F: F++; break;
				case G: G++; break;
				default:     break;
			}
		}		
		numberOfItems.put("A", A);
		numberOfItems.put("B", B);
		numberOfItems.put("C", C);
		numberOfItems.put("D", D);
		numberOfItems.put("E", E);
		numberOfItems.put("F", F);
		numberOfItems.put("G", G);
		
		return new int[] {A,B,C,D,E,F,G};
	}
	
	
	/*********** Aris *****************/
	
	/**
	 * 
	 * @param bundleB
	 * @return the bundle to suitable format for MinB  and MaxB entries
	 */
	@SuppressWarnings("unused")
	private String getBundleString(List<Resource> bundleB) {
		return bundleB.toString().replaceAll("[\\[\\](){},\\s]","");
	}
	
	/**
	 * 
	 * @param MinB
	 * @param MaxB
	 * @param bundle
	 * @param newPrice
	 * 
	 * Updates the max and min in the bundle list of Auction B
	 */
	private void updateMinMaxOnB(Dictionary<String, Double> MinB, Dictionary<String, Double> MaxB, List<Resource> bundle, double newPrice) {
		String myBundle = getBundleString(bundle);
		if(maxB.get(myBundle) != null || minB.get(myBundle)!=null) {
			if(newPrice > maxB.get(myBundle)) {
				maxB.put(myBundle,newPrice);
			}else if(newPrice < maxB.get(myBundle)) {
				minB.put(myBundle,newPrice);
			}
		}
	}
	
	/**
	 * 
	 * @param bundle
	 * @return the min value of specific bundle in B Auction
	 */
	@SuppressWarnings("unused")
	private double getMinB(List<Resource> bundle) {
		return minB.get(getBundleString(bundle));
	}
	
	/**
	 * 
	 * @param bundle
	 * @return the max value of specific bundle in B Auction
	 */
	@SuppressWarnings("unused")
	private double getMaxB(List<Resource> bundle) {
		return maxB.get(getBundleString(bundle));
	}
	
	/**
	 * 
	 * @param minB
	 * @param maxB
	 * 
	 * Initialize the min and max list for Auction B 
	 */
	private void initalizeMinMaxOnB(Dictionary<String, Double> minB, Dictionary<String, Double> maxB) {
		minB.put("AA",200.0); maxB.put("AA",200.0);
		minB.put("AAA",300.0); maxB.put("AAA",300.0);
		minB.put("AAAA",400.0); maxB.put("AAAA",400.0);
		minB.put("AAB",200.0); maxB.put("AAB",200.0);
		minB.put("AJK",200.0); maxB.put("AJK",200.0);
		minB.put("BB",50.0); maxB.put("BB",50.0);
		minB.put("CCCDDD",1200.0); maxB.put("CCCDDD",1200.0);
		minB.put("CCDDAA",800.0); maxB.put("CCDDAA",800.0);
		minB.put("CCDDBB",600.0); maxB.put("CCDDBB",600.0);
		minB.put("EEEEEF",1600.0); maxB.put("EEEEEF",1600.0);
		minB.put("EEEEF",800.0); maxB.put("EEEEF",800.0);
		minB.put("EEEF",400.0); maxB.put("EEEF",400.0);
		minB.put("EEF",200.0); maxB.put("EEF",200.0);
		minB.put("FF",100.0); maxB.put("FF",100.0);
		minB.put("FJK",300.0); maxB.put("FJK",300.0);
		minB.put("ABCDEFJK",1400.0); maxB.put("ABCDEFJK",1400.0);		
		
	}
	
	/**
	 * 
	 * @param req
	 * @param wallet
	 * @param message
	 * 
	 * The random strategy to play for now.
	 */
	private void tempRandomStrategy(CallForBids req,Wallet wallet,JiacMessage message) {
		double bid = -1;
		if (Math.random() > 0.3) {
			if (req.getMode() == CfBMode.BUY) {
				bid = wallet.getCredits() * 0.02;
				send(new Bid(req.getAuctioneerId(), bidderId, req.getCallId(), bid), message.getSender());
			} 
			else {
				if (wallet.contains(req.getBundle())) {
					bid = req.getMinOffer();
					send(new Bid(req.getAuctioneerId(), bidderId, req.getCallId(), bid), message.getSender());
				}
			}
		}
	}
}
