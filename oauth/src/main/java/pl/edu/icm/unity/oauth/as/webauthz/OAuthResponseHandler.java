/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.io.IOException;

import pl.edu.icm.unity.idpcommon.EopException;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.vaadin.server.Page;
import com.vaadin.server.SynchronizedRequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

/**
 * Redirects the client's browser creating URL with Vaadin response (or error).
 * 
 * @author K. Benedyczak
 */
public class OAuthResponseHandler
{
	public void returnOauthResponse(AuthorizationResponse oauthResponse, boolean destroySession) throws EopException
	{
		VaadinSession session = VaadinSession.getCurrent(); 
		session.addRequestHandler(new SendResponseRequestHandler(destroySession));
		session.setAttribute(AuthorizationResponse.class, oauthResponse);
		Page.getCurrent().reload();
		throw new EopException();
	}
	
	public class SendResponseRequestHandler extends SynchronizedRequestHandler
	{
		private boolean destroySession;
		
		public SendResponseRequestHandler(boolean destroySession)
		{
			this.destroySession = destroySession;
		}

		@Override
		public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request, 
				VaadinResponse responseO) throws IOException
		{
			VaadinServletResponse response = (VaadinServletResponse) responseO;
			AuthorizationResponse oauthResponse = session.getAttribute(AuthorizationResponse.class);
			if (oauthResponse != null)
			{
				try
				{
					
					String redirectURL = oauthResponse.toURI().toString();
					response.sendRedirect(redirectURL);
				} catch (SerializeException e)
				{
					throw new IOException("Error: can not serialize error response", e);
				}
				cleanContext();
				if (destroySession)
					session.getSession().invalidate();
				return true;
			}
			
			return false;
		}
	}
	
	public static OAuthAuthzContext getContext()
	{
		WrappedSession httpSession = VaadinSession.getCurrent().getSession();
		OAuthAuthzContext ret = (OAuthAuthzContext) httpSession.getAttribute(
				OAuthParseServlet.SESSION_OAUTH_CONTEXT);
		if (ret == null)
			throw new IllegalStateException("No OAuth context in UI");
		return ret;
	}
	
	public static void cleanContext()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		vSession.setAttribute(AuthorizationResponse.class, null);
		WrappedSession httpSession = vSession.getSession();
		httpSession.removeAttribute(OAuthParseServlet.SESSION_OAUTH_CONTEXT);
	}
}
