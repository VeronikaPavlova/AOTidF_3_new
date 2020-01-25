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


import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import org.sercho.masp.space.event.SpaceEvent;
import org.sercho.masp.space.event.SpaceObserver;
import org.sercho.masp.space.event.WriteCallEvent;
import de.dailab.jiactng.agentcore.action.Action;

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
import de.dailab.jiactng.agentcore.comm.message.IJiacMessage;
import de.dailab.jiactng.aot.auction.beans.BidderBean.MessageObserver;

import de.dailab.jiactng.aot.auction.onto.InformBuy.BuyType;


/**
 * TODO Implement this class.
 *
 * You might also decide to split the logic of your bidder up onto several
 * agent beans, e.g. one for each type of auction. In this case, remember
 * to keep the agent's `Wallet` in synch between the different roles, e.g.
 * using the agent's memory, as seen for the auctioneer beans.
 */
public class BidderBeanStub extends AbstractAgentBean
{

	// Hashmaps
  private HashMap<Resource, Integer>  selling;

  private HashMap<StartAuction.Mode, Integer> auctioneerIds;
  private HashMap<StartAuction.Mode, ICommunicationAddress> auctioneerAddresses;

  private HashMap<StartAuction , Integer> toBeSold;

  /** to identify and ignore duplicate calls for registration */
  private Set<Integer> seenAuctions = new HashSet<>();
  
  // Wallet
  private Wallet wallet;

  // Bidder ID
  private String bidderId;

  // Message group where to send multi-cast messages to
  private String messageGroup;


  private String groupToken;

  private float totalItems;

  private int round_num =0;


  @Override
  public void doStart() throws Exception
  {
  	auctioneerIds = new HashMap<>();
    toBeSold = new HashMap<>();
    auctioneerAddresses = new HashMap<>();
    selling = new HashMap<>();

    System.out.println("Start Bidding Agent with ID " + bidderId);

    //Join Group
    Action joinGroup = retrieveAction(ICommunicationBean.ACTION_JOIN_GROUP);
    IGroupAddress group = CommunicationAddressFactory.createGroupAddress(messageGroup);
    this.invoke(joinGroup, new Serializable[]{group});

    //Listen to memory events
    this.memory.attach(new MessageObserver(), new JiacMessage());

  }

  

   @Override
  public void execute()
  {
	//Just Buying ATM

	 round_num +=1;

	  if (wallet == null) return;

	   log.info(wallet);



  }


  public void setMessageGroup(String messageGroup)
  {
    this.messageGroup = messageGroup;
  }

  public String getMessageGroup()
  {
    return messageGroup;
  }

  public void setbidderId(String bidderId)
  {
    this.bidderId = bidderId;
  }

  public String getbidderId()
  {
    return bidderId;
  }

  public void setgroupToken(String groupToken)
  {
    this.groupToken = groupToken;
  }

  public String getgroupToken()
  {
    return groupToken;
  }


@SuppressWarnings("serial")
class MessageObserver implements SpaceObserver<IFact>

{

	private void handleInitializeBidder(JiacMessage message, IFact payload)
		  {
		    InitializeBidder ib = (InitializeBidder) message.getPayload();
		    //StartAuction auction_info = (StartAuction) message.getPayload();

		    if (bidderId.equals(ib.getBidderId()))
		    {
		      log.info("Initialized bidder!");
		      wallet = ib.getWallet();
		      // Initialize budgets
		    }
		  }

    private void handleInformBuy(JiacMessage message, IFact payload)
		  {
			  InformBuy inform = (InformBuy) message.getPayload();
				if (inform.getType() == BuyType.WON)
				{
					synchronized (wallet)
					{
						wallet.add(inform.getBundle());
						wallet.updateCredits(- inform.getPrice());
						log.info("Bundle " + inform.getBundle() + " won by " + bidderId );
					}
				}

			  memory.remove(message);
		  }
    
    private void send(IFact payload, ICommunicationAddress receiver) {
		log.info(String.format("Sending %s to %s", payload, receiver));
		JiacMessage msg = new JiacMessage(payload);
		IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		invoke(sendAction, new Serializable[] {msg, receiver});
	}


	private void handleInformSell(JiacMessage message, IFact payload)
		  {
				InformSell inform = (InformSell) message.getPayload();
				if (inform.getType() == InformSell.SellType.SOLD)
				{
					synchronized (wallet)
					{
						wallet.remove(inform.getBundle());
						wallet.updateCredits(inform.getPrice());
					}
				}

				memory.remove(message);
		   }

    private void handleStartAuction(JiacMessage message, IFact payload)
		  {
        		    
		    StartAuction startAuction = (StartAuction) message.getPayload();
		    
		    totalItems = startAuction.getNumItems();
		    
		    auctioneerIds.put(startAuction.getMode(), startAuction.getAuctioneerId());
		  	auctioneerAddresses.put(startAuction.getMode(), message.getSender());

		  }

	private void handleCallForBids(JiacMessage message, IFact payload)
		  {
		    CallForBids cfbs = (CallForBids) payload;


		    //Just for Buyer right now

		    log.info(" For the Bundle: " + cfbs.getBundle());


		    float bid_guess = (float) (wallet.getCredits() / totalItems );

		    double toBid = howMuchToBid(cfbs.getBundle(),bid_guess, cfbs.getMinOffer());

		    if (toBid < cfbs.getMinOffer() || selling.getOrDefault(cfbs.getBundle(), 0) > 0)
		    {
		      log.info(bidderId + " IGNORING OFFER");
		      toBid = 0;
		    }

		    log.info(bidderId + " I OFFER: " + (int) Math.max(0, Math.min(wallet.getCredits(), toBid))
		              + " | AS A SUBSTITUTE FOR THE MIN : " + cfbs.getMinOffer());

		    double finalBid = Math.max(0, Math.min(wallet.getCredits(), toBid));

		    Bid bid = new Bid(cfbs.getAuctioneerId(),bidderId,cfbs.getCallId() , finalBid );
		    JiacMessage msg = new JiacMessage(bid);


		    IActionDescription sendAction = retrieveAction(ICommunicationBean.ACTION_SEND);
		    invoke(sendAction, new Serializable[] { msg, message.getSender() });
		  }

	private double howMuchToBid (List<Resource> bundle , float bid_guess, double min_offer)
	{
	
	 double internal_bid = 0;
	 
	 internal_bid = bid_guess * ( Math.random() + 0.3 ) + ( min_offer * Math.random() ) ;

	 return internal_bid;

 	 }

 	@SuppressWarnings("rawtypes")
	@Override
	public void notify(SpaceEvent<? extends IFact> event)
  {
			// check the type of the event
		      if (event instanceof WriteCallEvent)
		      {
		        JiacMessage message = (JiacMessage) ((WriteCallEvent) event).getObject();
		    
		        		
		        log.info("Assigned to Bidder " + bidderId + ": " + message.getPayload());
		        log.info(" Current wallet: " + wallet);

		        IFact payload = message.getPayload();
		        
		        if (message.getPayload() instanceof StartAuctions &&
				seenAuctions.add(((StartAuctions) message.getPayload()).getAuctionsId())) 
		        {
		        	//handleRegistrationRequest
		        	send(new Register(bidderId, groupToken), message.getSender());
		        }

		        if (payload instanceof StartAuction)
		        {
		          handleStartAuction(message, payload);
		        }
		        else if (payload instanceof InitializeBidder)
		        {
		          handleInitializeBidder(message, payload);
		        }
		        else if (payload instanceof CallForBids)
		        {
		          handleCallForBids(message, payload);
		        }
		        else if (payload instanceof InformBuy)
		        {
		          handleInformBuy(message, payload);
		        }
		        else if (payload instanceof InformSell)
		        {
		          handleInformSell(message, payload);
		        }
		        else if (payload instanceof EndAuction)
		        {
		          log.info(bidderId + " " + wallet);
		        }
		        memory.remove(message);
		      }

		}


	}//From Observer

}//From BidBean

