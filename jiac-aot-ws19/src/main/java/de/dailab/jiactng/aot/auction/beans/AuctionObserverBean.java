package de.dailab.jiactng.aot.auction.beans;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import de.dailab.jiactng.agentcore.AbstractAgentBean;
import de.dailab.jiactng.agentcore.action.Action;
import de.dailab.jiactng.agentcore.action.ActionResult;
import de.dailab.jiactng.agentcore.ontology.AgentDescription;
import de.dailab.jiactng.agentcore.ontology.IActionDescription;
import de.dailab.jiactng.agentcore.ontology.IAgentDescription;
import de.dailab.webserver.IWebserver;

/**
 * Providing a HTTP servlet showing the current state of the auction.
 * This is used for comfortably monitoring the running auction server.
 * You do not really have to care about this.
 */
public class AuctionObserverBean extends AbstractAgentBean {

	/** the "secret" token for calling actions at the auctioneer meta bean */
	private String secretToken;
	
	private IWebserver webserver;

	private ServletContextHandler handler;

	@Override
	public void doStart() throws Exception {
		super.doStart();

		if (webserver == null) {
			webserver = thisAgent.getAgentNode().findAgentNodeBean(IWebserver.class);
		}
		handler = new ServletContextHandler();
		handler.setContextPath("/auctionobserver");
		handler.addServlet(new ServletHolder(new ExampleServlet()), "/");
		synchronized (webserver) {
			webserver.addHandler(handler);
		}
	}

	@Override
	public void doStop() throws Exception {
		if (webserver != null) {
			synchronized (webserver) {
				webserver.removeHandler(handler);
			}
		}
		super.doStop();
	}


	/**
	 * Very simple servlet for showing auctioneer status information in HTML
	 */
	protected class ExampleServlet extends HttpServlet {
		
		private static final long serialVersionUID = -6611233406871881867L;

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			BufferedWriter buff = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
			buff.append("<html><head><title>Auction Observer</title></head><body>");
			
			AgentDescription agTmplt = new AgentDescription(null, "AuctioneerAgent", null, null, null, null);
			List<IAgentDescription> agents = thisAgent.searchAllAgents(agTmplt);
			buff.append(String.format("<p>Running Auctioneers: %d</p>", agents.size()));
			
			Action template = new Action(AuctioneerMetaBean.ACTION_GET_STATE);
			List<IActionDescription> actions = thisAgent.searchAllActions(template);
			for (IActionDescription action : actions) {
				buff.append("<p>State:</p>");

				ActionResult result = invokeAndWaitForResult(action, new Serializable[] {secretToken}, 5000L);
				if (result.getFailure() == null) {
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Map<String, Map<String, Object>> map = (Map) result.getResults()[0];
					buff.append("<ul>");
					for (String key : map.keySet()) {
						Map<String, Object> inner = map.get(key);
						buff.append("<li>" + key + "</li>");
						buff.append("<ul>");
						for (String key2 : inner.keySet()) {
							buff.append("<li>" + key2 + ": " + inner.get(key2) + "</li>");
						}
						buff.append("</ul>");
					}
					buff.append("</ul>");
				} else {
					buff.append("Could not get state from auctioneer: " + result.getFailure());
					((Exception) result.getFailure()).printStackTrace();
					
				}
			}
			buff.append("</body></html>");

			buff.flush();
			buff.close();
			response.setContentType("text/html; charset=UTF-8");
			response.flushBuffer();
		}

	}
	
	public void setSecretToken(String secretToken) {
		this.secretToken = secretToken;
	}
}
