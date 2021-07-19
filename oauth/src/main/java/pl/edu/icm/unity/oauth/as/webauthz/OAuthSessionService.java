/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.util.URLUtils;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthContexts.ContextKey;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;

/**
 * Provides unified API to perform OAuth session bootstrap and cleanup.
 */
@Component
class OAuthSessionService 
{
	/**
	 * Under this key the OAuthContext object is stored in the session.
	 */
	private static final String SESSION_OAUTH_CONTEXT = "oauth2AuthnContextKey";
	static final String URL_PARAM_CONTEXT_KEY = "ctx";
	
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_OAUTH, OAuthSessionService.class);
	private final SessionManagement sessionMan;
	
	OAuthSessionService(SessionManagement sessionMan)
	{
		this.sessionMan = sessionMan;
	}
	
	static ContextKey setContext(HttpSession session, OAuthAuthzContext context)
	{
		ContextKey key;
		OAuthContexts sessionContext = (OAuthContexts) session.getAttribute(SESSION_OAUTH_CONTEXT);
		if (sessionContext == null)
		{
			sessionContext = new OAuthContexts();
			session.setAttribute(SESSION_OAUTH_CONTEXT, sessionContext);
			
			key = ContextKey.DEFAULT;
		} else
		{
			key = ContextKey.randomKey();
		}
		sessionContext.put(key, context);
		return key;
	}

	static Optional<OAuthAuthzContext> getContext(HttpServletRequest request)
	{
		return getContext(new HttpContextSession(request));
	}
	
	static OAuthAuthzContext getVaadinContext()
	{
		return getContextFromVaadinSession().orElseThrow(noOAuthContextException());
	}
	
	static boolean hasVaadinContext()
	{
		Optional<OAuthContextSession> session = VaadinContextSession.getCurrent();
		if (!session.isPresent())
			return false;
		
		return getContextFromVaadinSession().isPresent();
	}
	
	private static Optional<OAuthAuthzContext> getContextFromVaadinSession()
	{
		OAuthContextSession session = VaadinContextSession.getCurrent().orElseThrow(noOAuthContextException());
		return getContext(session);
	}
	
	private static Optional<OAuthAuthzContext> getContext(OAuthContextSession session)
	{
		OAuthContexts contexts = (OAuthContexts) session.getAttribute(SESSION_OAUTH_CONTEXT);
		if (contexts == null)
			return Optional.empty();
		
		OAuthAuthzContext ctx = contexts.get(session.get());
		if (ctx == null)
			return Optional.empty();

		return Optional.of(ctx);
	}
	
	void cleanupComplete(Optional<OAuthContextSession> session, boolean invalidateSSOSession)
	{
		cleanupBeforeResponseSent(session);
		cleanupAfterResponseSent(session, invalidateSSOSession);
	}
	
	void cleanupBeforeResponseSent(Optional<OAuthContextSession> session)
	{
		session.ifPresent(ses -> cleanupBeforeResponseSent(ses));
	}
	
	void cleanupAfterResponseSent(Optional<OAuthContextSession> session, boolean invalidateSSOSession)
	{
		cleanupAfterResponseSent(session.orElse(null), invalidateSSOSession);
	}
	
	void cleanupBeforeResponseSent(OAuthContextSession session)
	{
		LOG.trace("Cleaning OAuth session auto-proxy state");
		session.removeAttribute(ProxyAuthenticationFilter.AUTOMATED_LOGIN_FIRED);
	}
	
	void cleanupAfterResponseSent(OAuthContextSession session, boolean invalidateSSOSession)
	{
		LOG.trace("Cleaning OAuth session (sso logout={})", invalidateSSOSession);
		if (session != null)
		{
			OAuthContexts contexts = (OAuthContexts) session.getAttribute(SESSION_OAUTH_CONTEXT);
			LOG.trace("OAuth contexts: {}", contexts);
			if (contexts != null)
			{
				ContextKey key = session.get();
				contexts.remove(key);
				if (contexts.isEmpty())
				{
					LOG.trace("Removing {} from session", SESSION_OAUTH_CONTEXT);
					session.removeAttribute(SESSION_OAUTH_CONTEXT);
				} else
				{
					LOG.trace("Removing {} key from context", key);
				}
			}
			
			session.removeAttribute(AuthorizationResponse.class.getName());
		}
		if (invalidateSSOSession)
		{
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			sessionMan.removeSession(loginSession.getId(), true);
		}
	}
	
	static Supplier<IllegalStateException> noOAuthContextException()
	{
		return () -> new IllegalStateException("No OAuth context after authentication");
	}
	
	interface OAuthContextSession
	{
		void removeAttribute(String name);
		Object getAttribute(String name);
		ContextKey get();
	}
	
	
	static class HttpContextSession implements OAuthContextSession
	{
		private final HttpServletRequest request;
		
		HttpContextSession(HttpServletRequest request) 
		{
			this.request = request;
		}

		@Override
		public void removeAttribute(String name) 
		{
			request.getSession().removeAttribute(name);
		}

		@Override
		public Object getAttribute(String name)
		{
			return request.getSession().getAttribute(name);
		}

		@Override
		public ContextKey get()
		{
			String key = request.getParameter(URL_PARAM_CONTEXT_KEY);
			if (key != null)
				return new ContextKey(key);
			return ContextKey.DEFAULT;
		}
	}

	static class VaadinContextSession implements OAuthContextSession
	{
		private final WrappedSession session;
		
		static Optional<OAuthContextSession> getCurrent()
		{
			VaadinSession current = VaadinSession.getCurrent();
			return Optional.ofNullable(current == null ? null : new VaadinContextSession(current.getSession()));
		}
		
		private VaadinContextSession(WrappedSession session) 
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

		@Override
		public ContextKey get()
		{
			String queryString = UI.getCurrent().getPage().getLocation().getQuery();
			Map<String, List<String>> params = URLUtils.parseParameters(queryString);
			List<String> keys = params.get(URL_PARAM_CONTEXT_KEY);
			if (!CollectionUtils.isEmpty(keys))
				return new ContextKey(keys.get(0)); 
			return ContextKey.DEFAULT;
		}
	}
}
