/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthenticationContextManagement.UnboundRelayStateException;
import pl.edu.icm.unity.engine.api.authn.remote.SharedRemoteAuthenticationContextStore;
import pl.edu.icm.unity.engine.api.utils.URIBuilderFixer;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilterV8;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Awaits OAuth responses and handles them. The responses have their state extracted and OAuthn context 
 * is matched by it. If found then processing is redirected to the return URL associated with the context. 
 * 
 * @author K. Benedyczak
 */
public class ResponseConsumerServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ResponseConsumerServlet.class);
	public static final String PATH = "/oauth2ResponseConsumer";
	
	private final OAuthContextsManagement contextManagement;
	private final SharedRemoteAuthenticationContextStore remoteAuthnContextStore;

	public ResponseConsumerServlet(OAuthContextsManagement contextManagement, 
			SharedRemoteAuthenticationContextStore remoteAuthnContextStore)
	{
		this.contextManagement = contextManagement;
		this.remoteAuthnContextStore = remoteAuthnContextStore;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		String state = req.getParameter("state");		
		
		if (state == null)
		{
			log.warn("Got a request to the OAuth response consumer endpoint " +
					"without state parameter");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong HTTP request - no state");
			return;
		}
		
		OAuthContext context;
		try
		{
			context = contextManagement.getAndRemoveAuthnContext(state);
		} catch (UnboundRelayStateException e)
		{
			log.warn("Got a request to the OAuth response consumer endpoint " +
					"with invalid state parameter");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong HTTP request - invalid state");
			return;
		}
		
		String error = req.getParameter("error");
		if (error != null)
		{
			String desc = req.getParameter("error_description");
			log.warn("Got error OAuth response: " + error);
			context.setErrorCode(error);
			context.setErrorDescription(desc);
		} else
		{
			context.setAuthzCode(req.getParameter("code"));
		}
		remoteAuthnContextStore.addAuthnContext(context);
		log.debug("Received OAuth response for authenticator {} with valid state {}, redirecting to {}", 
				context.getAuthenticationStepContext().authnOptionId, state, context.getReturnUrl());
		resp.sendRedirect(getRedirectWithContextIdParam(context.getReturnUrl(), state));
	}
	
	private String getRedirectWithContextIdParam(String returnURL, String relayState) throws IOException
	{
		try
		{
			URIBuilder uriBuilder = URIBuilderFixer.newInstance(returnURL);
			uriBuilder.addParameter(RemoteRedirectedAuthnResponseProcessingFilterV8.CONTEXT_ID_HTTP_PARAMETER, relayState);
			return uriBuilder.build().toString();
		} catch (URISyntaxException e)
		{
			throw new IOException("Can't build return URL", e);
		}
	}
}
