/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.client.OAuthContext;
import io.imunity.vaadin.auth.idp.AbstractRedirectRequestHandler;

import java.io.IOException;


/**
 * Initializes OAuth login by redirecting to a given URL of OAuth2 AS.
 * @author K. Benedyczak
 */
public class RedirectRequestHandler extends AbstractRedirectRequestHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, RedirectRequestHandler.class);
	static final String REMOTE_AUTHN_CONTEXT = OAuth2Retrieval.class.getName()+".authnContext";
	
	public RedirectRequestHandler()
	{
		super(REMOTE_AUTHN_CONTEXT);
	}

	@Override
	protected boolean handleRequestInternal(Object contextO, VaadinSession vaadinSession,
	                                        VaadinRequest request, VaadinResponse response) throws IOException
	{
		OAuthContext context = (OAuthContext) contextO;
		VaadinServletResponse rr = (VaadinServletResponse) response;
		setCommonHeaders(response);
		String redirectURL = context.getRequestURI().toString();
		log.info("Starting OAuth redirection to OAuth provider " + redirectURL);
		rr.sendRedirect(redirectURL);
		return true;
	}
}
