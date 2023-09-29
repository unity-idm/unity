/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import com.google.common.base.MoreObjects;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

import pl.edu.icm.unity.base.utils.Log;

import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.function.Supplier;

public class LoginInProgressService<AUTHZ_CTX>
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_OAUTH, LoginInProgressService.class);
	
	public static final String URL_PARAM_CONTEXT_KEY = "signInId";
	
	private final String contextAttributeName;
	
	public LoginInProgressService(String contextAttributeName)
	{
		this.contextAttributeName = contextAttributeName;
	}

	public SignInContextKey setContext(HttpSession session, AUTHZ_CTX context)
	{
		SignInContextKey key;
		SignInContexts<AUTHZ_CTX> sessionContext = getAttribute(new HttpContextSession(session));
		if (sessionContext == null)
		{
			sessionContext = new SignInContexts<>();
			session.setAttribute(contextAttributeName, sessionContext);
			
			key = SignInContextKey.DEFAULT;
		} else
		{
			key = SignInContextKey.randomKey();
		}
		sessionContext.put(key, context);
		return key;
	}
	
	public Optional<AUTHZ_CTX> getContext(SignInContextSession session)
	{
		SignInContexts<AUTHZ_CTX> contexts = getAttribute(session);
		if (contexts == null)
			return Optional.empty();
		
		AUTHZ_CTX ctx = contexts.get(session.get());
		if (ctx == null)
			return Optional.empty();

		return Optional.of(ctx);
	}

	public Optional<AUTHZ_CTX> getContext(HttpServletRequest request)
	{
		return getContext(new HttpContextSession(request));
	}
	
	public AUTHZ_CTX getVaadinContext()
	{
		return getContextFromVaadinSession().orElseThrow(noSignInContextException());
	}
	
	public boolean hasVaadinContext()
	{
		Optional<SignInContextSession> session = VaadinContextSession.getCurrent();
		if (!session.isPresent())
			return false;
		
		return getContextFromVaadinSession().isPresent();
	}
	
	private Optional<AUTHZ_CTX> getContextFromVaadinSession()
	{
		SignInContextSession session = VaadinContextSession.getCurrent().orElseThrow(noSignInContextException());
		return getContext(session);
	}
	
	@SuppressWarnings("unchecked")
	public SignInContexts<AUTHZ_CTX> getAttribute(SignInContextSession session)
	{
		return (SignInContexts<AUTHZ_CTX>) session.getAttribute(contextAttributeName);
	}
	
	public void cleanUpSignInContextAttribute(SignInContextSession session)
	{
		if (session != null)
		{
			SignInContexts<AUTHZ_CTX> contexts = getAttribute(session);
			LOG.trace("SignIn contexts: {}", contexts);
			if (contexts != null)
			{
				SignInContextKey key = session.get();
				contexts.remove(key);
				if (contexts.isEmpty())
				{
					LOG.trace("Removing {} from session", contextAttributeName);
					session.removeAttribute(contextAttributeName);
				} else
				{
					LOG.trace("Removing {} key from context", key);
				}
			}
		}
	}
	
	public static Supplier<IllegalStateException> noSignInContextException()
	{
		return () -> new IllegalStateException("No sign in context after authentication");
	}

	public interface SignInContextSession
	{
		void removeAttribute(String name);
		Object getAttribute(String name);
		SignInContextKey get();
	}

	public static class SignInContexts<T>
	{
		private final Map<SignInContextKey, T> contexts;

		public SignInContexts()
		{
			this.contexts = new HashMap<>();
		}

		public void put(SignInContextKey key, T ctx)
		{
			contexts.put(key, ctx);
		}

		public T get(SignInContextKey ctxKey)
		{
			return contexts.get(ctxKey);
		}

		public void remove(SignInContextKey key)
		{
			contexts.remove(key);
		}

		public boolean isEmpty()
		{
			return contexts.isEmpty();
		}

		@Override
		public String toString()
		{
			return MoreObjects.toStringHelper(this).add("contexts", contexts).toString();
		}

		@Override
		public int hashCode()
		{
			return Objects.hashCode(contexts);
		}

		@Override
		public boolean equals(Object object)
		{
			if (object instanceof SignInContexts)
			{
				SignInContexts<?> that = (SignInContexts<?>) object;
				return Objects.equals(this.contexts, that.contexts);
			}
			return false;
		}
	}

	public static class SignInContextKey
	{
		public static final SignInContextKey DEFAULT = new SignInContextKey("default");

		public final String key;

		public SignInContextKey(String key)
		{
			this.key = key;
		}

		public static SignInContextKey randomKey()
		{
			return new SignInContextKey(UUID.randomUUID().toString());
		}

		@Override
		public int hashCode()
		{
			return Objects.hashCode(key);
		}

		@Override
		public boolean equals(Object object)
		{
			if (object instanceof SignInContextKey)
			{
				SignInContextKey that = (SignInContextKey) object;
				return Objects.equals(this.key, that.key);
			}
			return false;
		}
	}

	public static class HttpContextSession implements SignInContextSession
	{
		private final SignInContextKey key;
		private final HttpSession session;

		public HttpContextSession(HttpServletRequest request)
		{
			this.key = get(request);
			this.session = request.getSession();
		}

		private static SignInContextKey get(HttpServletRequest request)
		{
			String key = request.getParameter(URL_PARAM_CONTEXT_KEY);
			if (key != null)
				return new SignInContextKey(key);
			return SignInContextKey.DEFAULT;
		}

		public HttpContextSession(HttpSession session)
		{
			this.session = session;
			this.key = null;
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
		public SignInContextKey get()
		{
			return key;
		}
	}

	public static class VaadinContextSession implements SignInContextSession
	{
		private final WrappedSession session;
		
		public static Optional<SignInContextSession> getCurrent()
		{
			VaadinSession current = VaadinSession.getCurrent();
			return Optional.ofNullable(
					current == null || current.getSession() == null
						? null 
						: new VaadinContextSession(current.getSession()));
		}
		
		public VaadinContextSession(WrappedSession session) 
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
		public SignInContextKey get()
		{
			SignInContextKey key = SignInContextKey.DEFAULT;
			
			Map<String, String[]> queryString = UI.getCurrent() != null
					? VaadinService.getCurrentRequest().getParameterMap()
					: Map.of();

			if (queryString.get(URL_PARAM_CONTEXT_KEY) != null && queryString.get(URL_PARAM_CONTEXT_KEY).length == 1)
				key = new SignInContextKey(queryString.get(URL_PARAM_CONTEXT_KEY)[0]);

			return key;
		}
	}
}
