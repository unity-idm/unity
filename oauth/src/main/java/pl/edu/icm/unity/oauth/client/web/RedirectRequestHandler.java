/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.io.IOException;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.server.utils.Log;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;


/**
 * Initializes OAuth login by redirecting to a given URL of OAuth2 AS.
 * @author K. Benedyczak
 */
public class RedirectRequestHandler implements RequestHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, RedirectRequestHandler.class);
	public static final String TRIGGERING_PARAMETER = "redirectToIdP";
	
	@Override
	public boolean handleRequest(VaadinSession vaadinSession, VaadinRequest request,
			VaadinResponse response) throws IOException
	{
		String fire = request.getParameter(TRIGGERING_PARAMETER); 
		if (fire == null || !fire.equals(Boolean.TRUE.toString()))
			return false;
		
		WrappedSession session = vaadinSession.getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context == null)
		{
			log.warn("Got a request for the redirection, " +
					"but no OAuth2 context is present in the session.");
			return false;
		}
		
		handleRedirect(context, response);
		return true;
	}
	
	public static String getParam()
	{
		return RedirectRequestHandler.TRIGGERING_PARAMETER + "=true";
	}
	
	private void handleRedirect(OAuthContext context, VaadinResponse response) throws IOException
	{
		VaadinServletResponse rr = (VaadinServletResponse) response;
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
		String redirectURL = context.getRequestURI().toString();
		if (log.isDebugEnabled())
		{
			log.debug("Starting OAuth redirection to OAuth provider " + redirectURL);
		}
		rr.sendRedirect(redirectURL);
	}
}
