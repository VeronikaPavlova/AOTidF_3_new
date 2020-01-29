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
import org.springframework.util.ResizableByteArrayOutputStream;

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
import de.dailab.jiactng.aot.auction.onto.Offer;
import de.dailab.jiactng.aot.auction.onto.Register;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
import de.dailab.jiactng.aot.auction.onto.StartAuctions;
import de.dailab.jiactng.aot.auction.onto.Wallet;
import de.dailab.jiactng.aot.auction.onto.CallForBids.CfBMode;
import de.dailab.jiactng.aot.auction.onto.InformBuy.BuyType;

/**
 * TODO Implement this class.
 * 
 * You might also decide to split the logic of your bidder up onto several
 * agent beans, e.g. one for each type of auction. In this case, remember
 * to keep the agent's `Wallet` in synch between the different roles, e.g.
 * using the agent's memory, as seen for the auctioneer beans.
 */
public class BidderBeanNika extends AbstractAgentBean {

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
	
	private int roundCount =0;
	
	//Start Probabilities for each item. How much interest we have in each of the items in %
	private int probitemA = 75;
	private int probitemB = 0;
	private int probitemC = 50;
	private int probitemD = 50;
	private int probitemE = 110;
	private int probitemF = 100;
	private int probitemG = 0;
	private int probitemJ = 25; 
	private int probitemK = 25;
	 
	//Start maximal prices for each item
	private int priceA = 100;
	private int priceB = 25;
	private int priceC = 120;
	private int priceD = 120;
	private int priceE = 100;
	private int priceF = 80;
	private int priceJ = 40;
	private int priceK = 40;
	private int priceG = 1;
	
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
		roundCount +=1;
//		log.info("roundcount " + roundCount);
		if (wallet == null) return;
		log.info(wallet);
		
		if(roundCount > 70) {
			StartAuction.Mode C = StartAuction.Mode.C;
			if (auctioneerIds.containsKey(C)) {
				List<Resource> whatToOffer = new ArrayList<>();
				Resource[] resources = Resource.values();
				for(int i =0 ; i< resources.length; i++) {
					whatToOffer.add(resources[i]);
				}
				if (! whatToOffer.isEmpty()) {
					send(new Offer(auctioneerIds.get(C), bidderId, whatToOffer,  wallet.getCredits()), auctioneerAddresses.get(C));
				}
			}
		}
		// randomly offer some of the currently owned items for sale
//				StartAuction.Mode C = StartAuction.Mode.C;
//				if (auctioneerIds.containsKey(C)) {
//					List<Resource> whatToOffer = new ArrayList<>();
//					if(wallet.get(Resource.A) > 4) {
//						whatToOffer.add(Resource.A);
//					}
//					if(wallet.get(Resource.G) > 0) {
//						whatToOffer.add(Resource.G);
//					}
//					if(wallet.get(Resource.B) > 1) {
//						whatToOffer.add(Resource.B);
//					}
//					if(wallet.get(Resource.J) > 2) {
//						whatToOffer.add(Resource.J);
//					}
//					if(wallet.get(Resource.K) > 2) {
//						whatToOffer.add(Resource.K);
//					}
//					if (! whatToOffer.isEmpty()) {
//						send(new Offer(auctioneerIds.get(C), bidderId, whatToOffer, 0.02 * wallet.getCredits()), auctioneerAddresses.get(C));
//						// XXX TEST WITH FAULTY MESSAGES
////						send(new Offer(auctioneerIds.get(C), bidderId, whatToOffer, null), auctioneerAddresses.get(C));
//					}
//				}
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
			
			float[] ProbandPrice = BuyingProbability(cfb.getBundle());
			double price = (double) (ProbandPrice[1] + ((ProbandPrice[0] - 50)/100) * ProbandPrice[1]); 
			
			if(ProbandPrice[0] > 50) {
				log.info("Try to buy this bundle! Probability: " + ProbandPrice[0] + 
						" with price " + price);
				send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), 
						price), message.getSender());
			}
		}


		private float[] BuyingProbability(List<Resource> bundle) {
			float prob = 0;
			float price = 0;
			for(Resource res: bundle) {
				switch (res) {
				case A: prob += probitemA;
						price += priceA;
						break;
				case B: prob += probitemB;
						price += priceB;
						break;
				case C: prob += probitemC;
						price += priceC;
						break;
				case D: prob += probitemD;
						price += priceD;
						break;
				case E: prob += probitemE;
						price += priceE;
						break;
				case F: prob += probitemF;
						price += priceF;
						break;
				case G: prob += probitemG;
						price += priceG;
						break;
				case J: prob += probitemJ;
						price += priceJ;
						break;
				case K: prob += probitemK;
						price += priceK;
						break;
				}
			}
			prob /= bundle.size();
			return new float[] {prob, price};
		}

		private void handleCallForSell(CallForBids cfb, IJiacMessage message) {
			int numA = 0;
			int numB = 0;
			int numC = 0;
			int numD = 0;
			int numE = 0;
			int numF = 0;
			int numJ = 0;
			int numK = 0;

			for(Resource obj: cfb.getBundle()) {
				switch(obj) {
				case A: numA++;
						break;
				case B: numB++;
						break;
				case C: numC++;
						break;
				case D: numD++;
						break;
				case E: numE++;
						break;
				case F: numF++;
						break;
				case J: numJ++;
						break;
				case K: numK++;
						break;
				default:
					break;				
				}				
			}
			if (wallet.contains(cfb.getBundle())) {
				log.info("Nika I have the bundle " + cfb.getBundle());
//				send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());
				
				String bla = wallet.toString();
				//At the end sell every bundle 
				if(roundCount > 90) {
					send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());
				}
				//Sell the bundles with most value at the biden and middle
				else if(roundCount > 45) {
					if(wallet.get(Resource.A)>4 && numA == 4) {
						log.info("Nika I will sell bundle [AAAA]");
						send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());
					}else if(wallet.get(Resource.E) > 5 && wallet.get(Resource.F) > 1 && numE==5 && numF==1) {
						log.info("Nika I will sell bundle [EEEEEF]");
						send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());
					}else if(wallet.get(Resource.A) > 1 && wallet.get(Resource.B) > 1 && wallet.get(Resource.C) > 1 && 
							wallet.get(Resource.D) > 1 && wallet.get(Resource.E) > 1 && wallet.get(Resource.F) > 1 &&
							wallet.get(Resource.J) > 1 && wallet.get(Resource.K) > 1 &&	numA==1 && numB==1 && 
							numC==1 && numD==1 && numE==1 && numF==1 && numJ==1 && numK==1) {
						log.info("Nika I will sell bundle [ABCDEFJK]");
						send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());	
					}else if(wallet.get(Resource.C) > 3 && wallet.get(Resource.D) > 3 && numC==3 && numD==3) {
						log.info("Nika I will sell bundle [CCCDDD]");
						send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());
					}else if(wallet.get(Resource.F) > 1 && wallet.get(Resource.J) > 1 && wallet.get(Resource.K) > 1 && numJ==1 && numK ==1 && numF==1) {
						log.info("Nika I will sell bundle [FJK]");
						send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());
					}else if(wallet.get(Resource.C) > 2 && wallet.get(Resource.D) > 2 && wallet.get(Resource.A) > 2 && numC==2 && numD==2 && numA==2) {
						log.info("Nika I will sell bundle [CCDDAA]");
						send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());
					}else if(wallet.get(Resource.A) > 1 && wallet.get(Resource.J) > 1 && wallet.get(Resource.K) > 1 && numJ==1 && numK ==1 && numF==1) {
						log.info("Nika I will sell bundle [AJK]");
						send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());
					
				}
				
				}
		}
			
			// randomly offer some of the currently owned items for sale
			
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

					if (cfb.getMode() == CfBMode.BUY) {
						handleCallForBuy(cfb, message);
					}
					if(cfb.getMode() == CfBMode.SELL) {
						handleCallForSell(cfb, message);
					}
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