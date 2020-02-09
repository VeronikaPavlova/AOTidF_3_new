package de.dailab.jiactng.aot.auction.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.comm.CommunicationAddressFactory;
import de.dailab.jiactng.agentcore.comm.ICommunicationAddress;
import de.dailab.jiactng.agentcore.comm.ICommunicationBean;
import de.dailab.jiactng.agentcore.comm.IGroupAddress;
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
import de.dailab.jiactng.aot.auction.onto.Offer;
import de.dailab.jiactng.aot.auction.onto.Register;
import de.dailab.jiactng.aot.auction.onto.Resource;
import de.dailab.jiactng.aot.auction.onto.StartAuction;
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
		
	//Group Token
	private String groupToken;
		
	/** mapping auction types to auctioneer IDs */
	private Map<StartAuction.Mode, Integer> auctioneerIds;
	private Map<StartAuction.Mode, ICommunicationAddress> auctioneerAddresses;
	

	/** to identify and ignore duplicate calls for registration */
	private Set<Integer> seenAuctions = new HashSet<>();
    private int biddersN = 1;

    private Map< List<Resource>, Double> minBundle = new HashMap<List<Resource>, Double>();
    private Map< List<Resource>, Double> maxBundle = new HashMap<List<Resource>, Double>();
    

	/** strategy of Bidder */
	private String bidderStrategy;
	
	private enum Fiscal {RICH, MEDIUM, POOR};
	
	private Fiscal fiscal;
	
	private double rich = 7500.00;
	private double medium = 5000.00;
	
	double cash;
	
	double myOffer = 0.00;
	
	//Probabilities of each item stored in a HashMap
	private Map<Resource, Double> minitemPrices = new HashMap<Resource, Double>();
	private Map<Resource, Double> probabilities = new HashMap<Resource, Double>();
	
	//Track rounds 
	private int currentRound;
	private int roundEndA;
	private int roundEndB;
	private int roundEndC;
	
	private Collection<Item> itemsofA;
	private Collection<Item> bundlesofB;
	
	private MessageObserver msob = new MessageObserver();


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
		this.memory.attach(msob, new JiacMessage());
		
	}
	
	@Override
	public void execute() {
		
		if (wallet == null) return;
		log.info(wallet);
		
		/**
		 * TODO Maybe play here around with the price we offer?
		 * Also I commented it out but we could also check if selling all items after Auctioneer A closes is an improvement. 
		 * But I sometimes got here an error, and I think that here you could have the mistake of negative Objects in Wallet
		 * 
		 * Just some Info:
		 * 
		 * currentRound variable counts the rounds, till A ends (so we have max. 150 rounds at currentRound == 150 Auctioneer A finished)
		 * Then we have 20 more rounds where C is running (so till 170)
		 * and 40 more rounds B is running (so 190)
		 * 
		 */
		
		
		StartAuction.Mode C = StartAuction.Mode.C;
		if (auctioneerIds.containsKey(C)) {
			
			List<Resource> whatToOffer = new ArrayList<>();
			
			for(Map.Entry<Resource, Double> entry: probabilities.entrySet()) {
//				as long as Action A is running, just offer the not desirable items
				if(currentRound < 150) {	
					if(entry.getValue() < 0 && wallet.get(entry.getKey()) > 0) {
						whatToOffer.add(entry.getKey());
					}
				} else {
					if(wallet.get(entry.getKey()) > 0) {
						whatToOffer.add(entry.getKey());
					}
				}
			}
			if (! whatToOffer.isEmpty()) {
				double price = msob.getPrice(whatToOffer);
				send(new Offer(auctioneerIds.get(C), bidderId, whatToOffer, price), auctioneerAddresses.get(C));
				
				log.info("Queen offer bundle " + whatToOffer + " for the price " + price);
			}
		}		
	}
	
	
	//send bids to the auctionees
	private void send(IFact payload, ICommunicationAddress receiver) {
		log.info(String.format("Sending %s to %s", payload, receiver));
		JiacMessage msg = new JiacMessage(payload);
		IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		invoke(sendAction, new Serializable[] {msg, receiver});
	}
	
	/**
	 * Getter and Setter Methods
	 * 
	 */
	
	public void setBidderStrategy(String bidderStrategy) {
		this.bidderStrategy = bidderStrategy;
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
				}
				//Inititialize our bidder and register to the META Auctioneer
				if (message.getPayload() instanceof InitializeBidder) {
					currentRound = 0;
					InitializeBidder initBidder = (InitializeBidder) message.getPayload();
					if(bidderId.equalsIgnoreCase(initBidder.getWallet().getBidderId())) {
						
						// get wallet, inspect items, init min,max for B Auctions
						wallet = initBidder.getWallet();
				        
					} else {
						biddersN = biddersN +1;
						return;
					}
				} 

				if (message.getPayload() instanceof EndAuction) {
					EndAuction endMsg = (EndAuction) message.getPayload();
					if (bidderId.equals(endMsg.getWinner())) {
						log.info("Queen WON!  :-)");
					} else {
						log.info("WE LOST. :-(");
					}
					log.info("Winner's wallet: " + endMsg.getWallet());
					log.info("Queen wallet: " + wallet.getCredits());
					auctioneerIds.clear();
					auctioneerAddresses.clear();
					wallet = null;
				}

				// AUCTIONEERS A, B, C
				
				
				/**
				 * If the Auction is B
				 * 	initialize min and max values of each bundle 
				 * 	save the bundle Collection
				 * 
				 * If Auction is A
				 * 	get the number of rounds
				 * 	save the bundles of A
				 * 
				 */
				if (message.getPayload() instanceof StartAuction) {
					StartAuction startAuction = (StartAuction) message.getPayload();
					
					// remember auctioneer ID and address
					auctioneerIds.put(startAuction.getMode(), startAuction.getAuctioneerId());
					auctioneerAddresses.put(startAuction.getMode(), message.getSender());
					
					if(startAuction.getAuctioneerId() == 2) {						
						bundlesofB = startAuction.getInitialItems();
						initializeMinMax(bundlesofB);
						minitemPrices = initItemPrices(bundlesofB);
						updateProbabilities(minitemPrices);
					
					} 
					else if (startAuction.getAuctioneerId() == 1) {
						roundEndA = startAuction.getNumItems();
						roundEndB = roundEndA + 40;
						roundEndC = roundEndA + 20;
						
						/**
						 * TODO Here I save all the items Auctioneer A provide (in itemsofA). 
						 * Maybe we could check in the end if we can create some specific bundles depending of the 
						 * items A will still sell. Maybe could also variate the probability on how much we the items from A want if we could make 
						 * a specific bundle
						 */
						//Save all provided items of A 
						itemsofA = startAuction.getInitialItems();
					}
				}
				
				if (message.getPayload() instanceof CallForBids) {
					CallForBids cfb = (CallForBids) message.getPayload();

					updateMinMaxOnB(cfb.getBundle(), cfb.getMinOffer());


					
					//every 15 rounds update the boundaries of rich, medium, poor 
					if(currentRound % 15 == 0) {
						rich = wallet.getCredits() - (wallet.getCredits()/3);
						medium = wallet.getCredits() - 2 * (wallet.getCredits()/3);	
					}
					
					if (cfb.getMode() == CfBMode.BUY) {
						if(cfb.getAuctioneerId() == 1) {
							currentRound ++;
							log.info("Current round " + currentRound);	
							
						}
						if(bundlesofB != null) {
							handleCallForBuy(cfb, message);
						}
					}
					if(cfb.getMode() == CfBMode.SELL) {
						if(bundlesofB!= null && minitemPrices != null) {
//							log.info("Item Prices " + minitemPrices);
							handleCallForSell(cfb, message);
						}
					}
				}
				
				if (message.getPayload() instanceof InformBuy) {
					InformBuy inform = (InformBuy) message.getPayload();
					if (inform.getType() == BuyType.WON) {
						synchronized (wallet) {
							wallet.add(inform.getBundle());
							wallet.updateCredits(- inform.getPrice());
							log.info("Queen buy the bundle " + inform.getBundle() + " for the price " + inform.getPrice());
						}
					}
				}
				
				if (message.getPayload() instanceof InformSell) {
					InformSell inform = (InformSell) message.getPayload();
					updateMinMaxOnB(inform.getBundle(), inform.getPrice());
					
					if (inform.getType() == InformSell.SellType.SOLD) {
						synchronized (wallet) {
							wallet.remove(inform.getBundle());
							wallet.updateCredits(inform.getPrice());

							log.info("Queen sold the bundle " + inform.getBundle() + " for the price " + inform.getPrice());
							}
						}
				}
								
				// once handled, the message should be removed from memory
				memory.remove(message);
			}
		}


		@SuppressWarnings("unused")
		private void initializeMinMax(Collection<Item> initialItems) {
			for(Item item: initialItems){
				minBundle.put(item.getBundle(), item.getPrice());
				maxBundle.put(item.getBundle(), item.getPrice());
			}			
		}
		/**
		 * TODO currenPrices and initItemPrices could be one function
		 */
		
		/**
		 * Compute the price of each item depending on the bundles from B 
		 * So that we can compute the best bundles independent if B Mode if Fixed or Random
		 */
		private Map<Resource, Double> initItemPrices(Collection<Item> initItemB) {
			
			Map<Resource, Double> itemPrices = new HashMap<Resource, Double>();

			for(int i =0; i < 2; i++) {
				for(Item item: initItemB) {
					List<Resource> bundle = item.getBundle();
						//Prices for A, B and F from the bundles AA, BB, FF
						if(bundle.size() == 2) {
							itemPrices.put(bundle.get(0), (maxBundle.get(bundle)/2));
						}
						//bundle CCCDDD get the price for C, D
						if(bundle.size() == 6 && bundle.get(5) == Resource.D) {
							//C
							itemPrices.put(bundle.get(0), (maxBundle.get(bundle)/6));
							itemPrices.put(bundle.get(3), (maxBundle.get(bundle)/6));
						}
						
						//bundle EEF get the price for E
						if(bundle.size() == 3 && bundle.get(2) == Resource.F && itemPrices.get(Resource.F) != null) {
							itemPrices.put(bundle.get(0), ( (maxBundle.get(bundle) - itemPrices.get(Resource.F)) /2));
						}
		
						//bundle AJK get the price for J, K
						if(bundle.size() == 3 && bundle.get(2) == Resource.K && bundle.get(0) == Resource.A && itemPrices.get(Resource.A) != null) {
							//J
							itemPrices.put(bundle.get(1), ((maxBundle.get(bundle) - itemPrices.get(Resource.A)) /2));
							//K
							itemPrices.put(bundle.get(2), ((maxBundle.get(bundle) - itemPrices.get(Resource.A)) /2));
					}
				}
			}
			itemPrices.put(Resource.G, 0.0);
			
			return itemPrices;
		}
		
		/**
		 * Compute the price of each item depending on the bundles from B 
		 * So that we can compute the best bundles independent if B Mode if Fixed or Random
		 */
		private Map<Resource, Double> currItemPrice() {
			
			Map<Resource, Double> itemPrices = new HashMap<Resource, Double>();

			for(int i =0; i < 2; i++) {
				for(List<Resource> bundle: maxBundle.keySet()) {
					//Prices for A, B and F from the bundles AA, BB, FF
					if(bundle.size() == 2) {
						itemPrices.put(bundle.get(0), (maxBundle.get(bundle)/2));
					}
					//bundle CCCDDD get the price for C, D
					if(bundle.size() == 6 && bundle.get(5) == Resource.D) {
						//C
						itemPrices.put(bundle.get(0), (maxBundle.get(bundle)/6));
						itemPrices.put(bundle.get(3), (maxBundle.get(bundle)/6));
					}
					
					//bundle EEF get the price for E
					if(bundle.size() == 3 && bundle.get(2) == Resource.F && itemPrices.get(Resource.F) != null) {
						itemPrices.put(bundle.get(0), ( (maxBundle.get(bundle) - itemPrices.get(Resource.F)) /2));
					}
	
					//bundle AJK get the price for J, K
					if(bundle.size() == 3 && bundle.get(2) == Resource.K && bundle.get(0) == Resource.A && itemPrices.get(Resource.A) != null) {
						//J
						itemPrices.put(bundle.get(1), ((maxBundle.get(bundle) - itemPrices.get(Resource.A)) /2));
						//K
						itemPrices.put(bundle.get(2), ((maxBundle.get(bundle) - itemPrices.get(Resource.A)) /2));
					}
				}
			}
			itemPrices.put(Resource.G, 0.0);
			
			return itemPrices;
		}
		
		/**
		 * After we have the prices for each item we can compute the difference between the item Price
		 * and our win of the current price of the bundles
		 * 
		 * All the differences we put into a list and sort the list 
		 * the higher the win the more desirable is to sell the bundle
		 */
		private void updateProbabilities(Map<Resource, Double> itemPrices) {
			
			Map<List<Resource>, Double> sortedByCount = bundlePriceDifference(itemPrices);
			
			//bundle with most win has the probability 100 and with each bund
			double probability = 100;
			double diffprob = probability/16;
			
			double probA, probB, probC, probD, probE, probF, probJ, probK, probG;
			probA = probB = probC = probD = probE = probF = probJ = probK = probG = 0;
			
			double countA, countB, countC, countD, countE, countF, countJ, countK;
			countA = countB = countC = countD = countE = countF = countJ = countK = 0;
			
			for(Map.Entry<List<Resource>, Double> entry: sortedByCount.entrySet()) {
				List<Resource> bundle = entry.getKey();
				for(Resource r: bundle) {
					switch (r) {
					case A:
						probA += probability;
						countA += 1;
						break;
					case B:
						probB += probability;
						countB += 1;
						break;
					case C:
						probC += probability;
						countC += 1;
						break;
					case D:
						probD += probability;
						countD += 1;
						break;
					case E:
						probE += probability;
						countE += 1;
						break;
					case F:
						probF += probability;
						countF += 1;
						break;
					case J:
						probJ += probability;
						countJ += 1;
						break;
					case K:
						probK += probability;
						countK += 1;
						break;
					default:
						break;
					}
				}
				double value = entry.getValue();
				
				/**
				 * TODO Play here around with the probabilities we give each bundle
				 * when the diff > 0 we just decrease [100, 100 -1/16, 100 - 2/16 ...]
				 * wenn diff = 0 the we have the probability 50
				 * and below 0 we again decrease from 50 [..., 50, 50 - 1/16, 50 - 2/16...]
				 * 
				 * Play here around
				 */
				if ( value > 0){
					probability = 100;
				}else if(value == 0) {
					probability = 50;
				}	else if(value < 0) {
					probability = 0;
				}
			}

			probabilities.put(Resource.A, probA/countA);
			probabilities.put(Resource.B, probB/countB);
			probabilities.put(Resource.C, probC/countC);
			probabilities.put(Resource.D, probD/countD);
			probabilities.put(Resource.E, probE/countE);
			probabilities.put(Resource.F, probF/countF);
			probabilities.put(Resource.J, probJ/countJ);
			probabilities.put(Resource.K, probK/countK);

			probabilities.put(Resource.G, probG);	
		}
		
		/**
		 * returns a list with all the bundles which we can sell to B and the difference in prices,
		 * sorted from the most win to the smallest win
		 * 
		 * @param itemPrices
		 * @return sorted Map List
		 */
		public Map<List<Resource>, Double> bundlePriceDifference(Map<Resource, Double> itemPrices) {
			Map<List<Resource>, Double> differences = new HashMap<List<Resource>, Double>();
			
			/**
			 * Compute all the differences between actual bundle price and the minOffer we get from B
			 * This is the difference
			 */
			double bundlediff;
			for(Item item: bundlesofB) {
				double sum = 0;
				for(Resource r: item.getBundle()) {
					sum += itemPrices.get(r);
				}
				bundlediff = maxBundle.get(item.getBundle()) - sum;
				differences.put(item.getBundle(), bundlediff);
			}
			
			//Sort the price differences from biggest to smalles
			Map<List<Resource>, Double> sortedByCount = differences.entrySet()
	                .stream()
	                .sorted((Map.Entry.<List<Resource>, Double>comparingByValue().reversed()))
	                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			
			return sortedByCount;
		}


		private void handleCallForSell(CallForBids cfb, JiacMessage message) {
			Map<List<Resource>, Double> sortedBundleDiff = bundlePriceDifference(minitemPrices);
			
			for(Map.Entry<List<Resource>, Double> entry: sortedBundleDiff.entrySet()) {
				double diff = entry.getValue();
				List<Resource> bundle = entry.getKey();
				//If Auction A ends, sell all bundles you have 
				if(currentRound < 150) {
					//if the bundle is preferable to be selled
					if(bundle.equals(cfb.getBundle()) && diff > 0) {
						//then check if we have the bundle and sell it
						if(wallet.contains(cfb.getBundle())){
							send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());

						}
					}
				} else {
					if(wallet.contains(cfb.getBundle())){
						send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), cfb.getMinOffer()), message.getSender());					}
				}
			}			
		}		

		/**
		 * Buying bundles from Auctioneer A 
		 * @param cfb
		 * @param message
		 */
		private void handleCallForBuy(CallForBids cfb, JiacMessage message) {
			
			//Probability how desirable the bundle is
			double probability = computeProbability(cfb.getBundle());
			
			double offer = strategy(probability, cfb.getBundle());

			if(offer != -1) {
				send(new Bid(cfb.getAuctioneerId(), bidderId, cfb.getCallId(), offer), message.getSender());

				log.info("Queen want buy " + cfb.getBundle() + " for the price " + offer);
			}
			
		}		


		private double strategy(double probability, List<Resource> bundle) {
			/**
			 * TODO Maybe difference between Auctioneer A and Auctioneer C while buying ?
			 * Also Play here around with the price and if we neccesarily need to have for different fiscal different prices?
			 */

			Fiscal fiscal = getFiscalMode();
			
			double price = getPrice(bundle);
			
//			price += price * 0.1;
			
			if(probability > 20 && fiscal == Fiscal.RICH) {
				return price;
			} else if (probability > 40 && fiscal == Fiscal.MEDIUM) {
				return price;
			} else if(probability > 60 && fiscal == Fiscal.POOR) {
				return price;
			} else {
				return -1;
			}
		}
		
		public double getPrice(List<Resource> bundle) {
			//depending on the curen bundle prices compute the current prices of each item new
			Map<Resource, Double> currPrices = currItemPrice();
			
			Map<Resource, Integer> numberofItems = bundleItemsCounter(bundle);

			double price = 0;
			
			for(Map.Entry<Resource, Integer> entry: numberofItems.entrySet()) {
				double count = entry.getValue();
				if(count > 0) {
					price += count * currPrices.get(entry.getKey());
				}
			}
			
			price += price * 0.1;
			
			return price;
		}


		private double computeProbability(List<Resource> bundle) {

			Map<Resource, Integer> numberofItems = bundleItemsCounter(bundle);

			updateProbabilities(minitemPrices);
			
			int count = 0;
			double probability = 0;
			for(Map.Entry<Resource, Integer> entry: numberofItems.entrySet()) {
				if(entry.getValue() != 0) {
					probability += entry.getValue() * probabilities.get(entry.getKey());
					count += 1;
				}
			}
			
			return probability /count;
		}		
	}
	
		/**
		 * return the current strategy depending on the money in the wallet
		 * @return
		 */
		public Fiscal getFiscalMode() {
			double currentCash = wallet.getCredits();
			
			if(currentCash > rich) {
				fiscal = Fiscal.RICH;
			}else if (currentCash <= rich && currentCash > medium) {
				fiscal = Fiscal.MEDIUM;
			}else {
				fiscal = Fiscal.POOR;
			}
		
			return fiscal;
		}
	
	//This will Return a Map with the Number of times each letter appear on the bundle	
		private Map<Resource, Integer> bundleItemsCounter(List<Resource> bundle) 
		{
			 /** To store the number of times an element appear in a bundle*/
			Map<Resource, Integer> numberOfItems = new HashMap<Resource, Integer>();
			
			Integer A=0, B=0, C=0, D=0, E=0, F=0, G=0, J=0, K=0;
			
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
					case J: J++; break;
					case K: K++; break;
					default:     break;
				}
			}		
			numberOfItems.put(Resource.A, A);
			numberOfItems.put(Resource.B, B);
			numberOfItems.put(Resource.C, C);
			numberOfItems.put(Resource.D, D);
			numberOfItems.put(Resource.E, E);
			numberOfItems.put(Resource.F, F);
			numberOfItems.put(Resource.G, G);
			numberOfItems.put(Resource.J, J);
			numberOfItems.put(Resource.K, K);
			
			return numberOfItems;
			
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
	private void updateMinMaxOnB(List<Resource> bundle, double newPrice) {
		/**
		 * TODO At the moment we don't use the minBundle. But maybe if we know an itemPrice decrease we could use this information
		 * to make it less desirable(?)
		 */
		if(maxBundle.get(bundle) != null || minBundle.get(bundle)!=null) {
//			if(newPrice > maxBundle.get(bundle)) {
				maxBundle.put(bundle, newPrice);
//			}else if(newPrice < maxBundle.get(bundle)) {
//				minBundle.put(bundle, newPrice);
//			}
		}
	}
}
