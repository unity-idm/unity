/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Awaits OAuth responses and handles them.
 * 
 * @author K. Benedyczak
 */
public class ResponseConsumerServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ResponseConsumerServlet.class);
	public static final String PATH = "/oauth2ResponseConsumer";
	
	private OAuthContextsManagement contextManagement;

	public ResponseConsumerServlet(OAuthContextsManagement contextManagement)
	{
		this.contextManagement = contextManagement;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		OAuthAuthzResponse oar = null;
		OAuthProblemException error = null;
		String state;
		try
		{
			oar = OAuthAuthzResponse.oauthCodeAuthzResponse(req);
			state = oar.getState();
		} catch (OAuthProblemException e)
		{
			log.debug("Got a request to the OAuth response consumer endpoint with error response", e);
			state = e.getState();
			error = e;
		}
		
		if (state == null)
		{
			log.warn("Got a rerequest to the OAuth response consumer endpoint " +
					"without state parameter");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong HTTP request - no state");
			return;
		}
		
		OAuthContext context;
		try
		{
			context = contextManagement.getAuthnContext(state);
		} catch (WrongArgumentException e)
		{
			log.warn("Got a request to the OAuth response consumer endpoint " +
					"with invalid state parameter");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong HTTP request - invalid state");
			return;
		}
		
		context.setAuthzResponse(oar);
		context.setError(error);
		resp.sendRedirect(context.getReturnUrl());
	}
}
