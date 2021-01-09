/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;

/**
 * Provides unified API to perform OAuth session bootstrap and cleanup.
 */
@Component
class OAuthSessionService 
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthSessionService.class);
	private final SessionManagement sessionMan;
	
	OAuthSessionService(SessionManagement sessionMan)
	{
		this.sessionMan = sessionMan;
	}

	static OAuthAuthzContext getVaadinContext()
	{
		return getContext(VaadinSessionAttributes.getCurrent().get());
	}
	
	static OAuthAuthzContext getContext(SessionAttributes session)
	{
		OAuthAuthzContext ret = (OAuthAuthzContext) session.getAttribute(
				OAuthParseServlet.SESSION_OAUTH_CONTEXT);
		if (ret == null)
			throw new IllegalStateException("No OAuth context after authentication");
		return ret;
	}

	
	static boolean hasVaadinContext()
	{
		return hasContext(VaadinSessionAttributes.getCurrent());
	}

	private static boolean hasContext(Optional<SessionAttributes> session)
	{
		if (!session.isPresent())
			return false;
		
		SessionAttributes httpSession = session.get();
		return httpSession.getAttribute(OAuthParseServlet.SESSION_OAUTH_CONTEXT) != null;
	}
	
	void cleanupComplete(Optional<SessionAttributes> session, boolean invalidateSSOSession)
	{
		cleanupBeforeResponseSent(session);
		cleanupAfterResponseSent(session, invalidateSSOSession);
	}
	
	void cleanupBeforeResponseSent(Optional<SessionAttributes> session)
	{
		session.ifPresent(ses -> cleanupBeforeResponseSent(ses));
	}
	
	void cleanupAfterResponseSent(Optional<SessionAttributes> session, boolean invalidateSSOSession)
	{
		cleanupAfterResponseSent(session.orElse(null), invalidateSSOSession);
	}
	
	void cleanupBeforeResponseSent(SessionAttributes session)
	{
		log.trace("Cleaning OAuth session auto-proxy state");
		session.removeAttribute(ProxyAuthenticationFilter.AUTOMATED_LOGIN_FIRED);
	}
	
	void cleanupAfterResponseSent(SessionAttributes session, boolean invalidateSSOSession)
	{
		log.trace("Cleaning OAuth session (sso logout={})", invalidateSSOSession);
		if (session != null)
		{
			session.removeAttribute(OAuthParseServlet.SESSION_OAUTH_CONTEXT);
			session.removeAttribute(AuthorizationResponse.class.getName());
		}
		if (invalidateSSOSession)
		{
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			sessionMan.removeSession(loginSession.getId(), true);
		}
	}
	
	interface SessionAttributes
	{
		void removeAttribute(String name);
		Object getAttribute(String name);
	}
	
	
	static class HttpSessionAttributes implements SessionAttributes
	{
		private final HttpSession session;
		
		HttpSessionAttributes(HttpSession session) 
		{
			this.session = session;
		}

		@Override
		public void removeAttribute(String name) 
		{
			session.removeAttribute(name);
		}

		@Override
		public Object getAttribute(String name)
		{
			return session.getAttribute(name);
		}
	}

	static class VaadinSessionAttributes implements SessionAttributes
	{
		private final WrappedSession session;
		
		public static Optional<SessionAttributes> getCurrent()
		{
			VaadinSession current = VaadinSession.getCurrent();
			return Optional.ofNullable(current == null ? null : new VaadinSessionAttributes(current.getSession()));
		}
		
		private VaadinSessionAttributes(WrappedSession session) 
		{
			this.session = session;
		}

		@Override
		public void removeAttribute(String name) 
		{
			session.removeAttribute(name);
		}

		@Override
		public Object getAttribute(String name)
		{
			return session.getAttribute(name);
		}
	}
}
