/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.io.IOException;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.authn.remote.AbstractRedirectRequestHandler;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;


/**
 * Initializes OAuth login by redirecting to a given URL of OAuth2 AS.
 * @author K. Benedyczak
 */
public class RedirectRequestHandler extends AbstractRedirectRequestHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, RedirectRequestHandler.class);
	
	public RedirectRequestHandler()
	{
		super(OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
	}

	@Override
	protected boolean handleRequestInternal(Object contextO, VaadinSession vaadinSession,
			VaadinRequest request, VaadinResponse response) throws IOException
	{
		OAuthContext context = (OAuthContext) contextO;
		VaadinServletResponse rr = (VaadinServletResponse) response;
		setCommonHeaders(response);
		String redirectURL = context.getRequestURI().toString();
		if (log.isDebugEnabled())
		{
			log.debug("Starting OAuth redirection to OAuth provider " + redirectURL);
		}
		rr.sendRedirect(redirectURL);
		return true;
	}
}
