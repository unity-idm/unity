/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import static com.vaadin.flow.shared.ApplicationConstants.UI_ID_PARAMETER;
import static io.imunity.vaadin.endpoint.common.SignInToUIIdContextBinder.getUrlParamSignInContextKey;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import com.google.common.base.MoreObjects;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import pl.edu.icm.unity.base.utils.Log;

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
		SignInContexts<AUTHZ_CTX> sessionContext = getAttribute(new HttpContextSession(session));
		if (sessionContext == null)
		{
			sessionContext = new SignInContexts<>();
			session.setAttribute(contextAttributeName, sessionContext);
		}
		
		SignInContextKey key = sessionContext.isEmpty()
				? UrlParamSignInContextKey.DEFAULT
				: UrlParamSignInContextKey.randomKey();
		
		sessionContext.put(key, context);
		return key;
	}
	
	public void putExistingContextUnderNewKey(WrappedSession wrappedSession, SignInContextKey existingKey, SignInContextKey newKey)
	{
		SignInContexts<AUTHZ_CTX> sessionContext = getAttribute(new VaadinContextSession(wrappedSession));
		if (sessionContext == null)
		{
			throw new NoSignInContextException(String.format("No signin contexts in session to map: %s -> %s",
					newKey.getKey(), existingKey.getKey()));
		}
		sessionContext.putExistingContextUnderNewKey(existingKey, newKey);
	}
	
	public Optional<AUTHZ_CTX> getContext(SignInContextSession session)
	{
		SignInContexts<AUTHZ_CTX> contexts = getAttribute(session);
		if (contexts == null)
		{
			return Optional.empty();
		}
		
		AUTHZ_CTX ctx = contexts.get(session.get());
		if (ctx == null)
		{
			return Optional.empty();
		}

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
		{
			return false;
		}
		
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
		return () -> new NoSignInContextException("No sign in context after authentication");
	}
	
	private static class NoSignInContextException extends IllegalStateException
	{
		NoSignInContextException(String message)
		{
			super(message);
		}
	}

	public interface SignInContextSession
	{
		void removeAttribute(String name);
		Object getAttribute(String name);
		SignInContextKey get();
	}

	static class SignInContexts<T>
	{
		private final Map<SignInContextKey, T> contexts;
		private final Map<SignInContextKey, List<SignInContextKey>> urlParamKeyToUIIdKeys;

		SignInContexts()
		{
			this.contexts = new HashMap<>();
			this.urlParamKeyToUIIdKeys = new HashMap<>();
		}

		void putExistingContextUnderNewKey(SignInContextKey existingKey, SignInContextKey newKey)
		{
			T context = contexts.get(existingKey);
			if (context == null)
			{
				throw new NoSignInContextException("No login context for " + existingKey.getKey());
			}
			contexts.put(newKey, context);
			urlParamKeyToUIIdKeys.computeIfAbsent(existingKey, __ -> new ArrayList<>()).add(newKey);
		}

		void put(SignInContextKey key, T ctx)
		{
			if (contexts.containsKey(key))
			{
				throw new IllegalStateException("context already exists for key: " + key.getKey());
			}
			contexts.put(key, ctx);
		}

		T get(SignInContextKey ctxKey)
		{
			return contexts.get(ctxKey);
		}

		void remove(SignInContextKey key)
		{
			StringBuilder removedKeys = new StringBuilder(key.getKey());
			contexts.remove(key);
			Optional.ofNullable(urlParamKeyToUIIdKeys.remove(key)).ifPresent(keys -> 
			{
				keys.forEach(contexts::remove);
				removedKeys.append(";").append(keys.stream().map(SignInContextKey::getKey).collect(joining(";")));
			});
			LOG.debug("removed keys: {}", removedKeys.toString());
		}

		boolean isEmpty()
		{
			return contexts.isEmpty();
		}

		@Override
		public String toString()
		{
			return MoreObjects.toStringHelper(this)
					.add("contexts", contexts)
					.add("urlParamKeyToUIIdKeys", urlParamKeyToUIIdKeys)
					.toString();
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
	
	public interface SignInContextKey
	{
		String getKey();
	}

	public static class VaadinUIIdSignInContextKey implements SignInContextKey
	{
		public final String key;
		
		public VaadinUIIdSignInContextKey(int uiId, WrappedSession session)
		{
			this.key = uiId + "@" + session.getId();
		}
		
		@Override
		public String getKey()
		{
			return key;
		}
	
		@Override
		public int hashCode()
		{
			return Objects.hashCode(key);
		}

		@Override
		public boolean equals(Object object)
		{
			if (object instanceof VaadinUIIdSignInContextKey that)
			{
				return Objects.equals(this.key, that.key);
			}
			return false;
		}
		
		@Override
		public String toString()
		{
			return key;
		}
	}
	
	public static class UrlParamSignInContextKey implements SignInContextKey
	{
		public static final UrlParamSignInContextKey DEFAULT = new UrlParamSignInContextKey("default");

		public final String key;

		public UrlParamSignInContextKey(String key)
		{
			this.key = key;
		}

		public static UrlParamSignInContextKey randomKey()
		{
			return new UrlParamSignInContextKey(UUID.randomUUID().toString());
		}
		
		@Override
		public String getKey()
		{
			return key;
		}

		@Override
		public int hashCode()
		{
			return Objects.hashCode(key);
		}

		@Override
		public boolean equals(Object object)
		{
			if (object instanceof UrlParamSignInContextKey that)
			{
				return Objects.equals(this.key, that.key);
			}
			return false;
		}
		
		@Override
		public String toString()
		{
			return key;
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
			{
				return new UrlParamSignInContextKey(key);
			}
			return UrlParamSignInContextKey.DEFAULT;
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
			return getVaadinUIIdSignInContextKey()
					.orElse(getUrlParamSignInContextKey());
		}

		private Optional<SignInContextKey> getVaadinUIIdSignInContextKey()
		{
			Map<String, String[]> queryString = VaadinService.getCurrentRequest().getParameterMap();
			if (queryString.get(UI_ID_PARAMETER) != null && queryString.get(UI_ID_PARAMETER).length == 1)
			{
				Integer uiId = Integer.valueOf(queryString.get(UI_ID_PARAMETER)[0]);
				return Optional.of(new VaadinUIIdSignInContextKey(uiId, session));
			}
			return Optional.empty();
		}
	}
}

