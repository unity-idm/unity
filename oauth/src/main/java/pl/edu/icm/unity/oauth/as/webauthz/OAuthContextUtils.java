/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;

/**
 * Helper methods to handle {@link OAuthAuthzContext}.
 * @author K. Benedyczak
 */
public class OAuthContextUtils
{
	public static OAuthAuthzContext getContext()
	{
		WrappedSession httpSession = VaadinSession.getCurrent().getSession();
		OAuthAuthzContext ret = (OAuthAuthzContext) httpSession.getAttribute(
				OAuthParseServlet.SESSION_OAUTH_CONTEXT);
		if (ret == null)
			throw new IllegalStateException("No OAuth context in UI");
		return ret;
	}

	public static boolean hasContext()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		if (vSession == null)
			return false;
		
		WrappedSession httpSession = vSession.getSession();
		OAuthAuthzContext ret = (OAuthAuthzContext) httpSession.getAttribute(
				OAuthParseServlet.SESSION_OAUTH_CONTEXT);
		return ret != null;
	}
	
	public static void cleanContext()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		if (vSession != null)
		{
			vSession.setAttribute(AuthorizationResponse.class, null);
			WrappedSession httpSession = vSession.getSession();
			httpSession.removeAttribute(OAuthParseServlet.SESSION_OAUTH_CONTEXT);
		}
	}
}
