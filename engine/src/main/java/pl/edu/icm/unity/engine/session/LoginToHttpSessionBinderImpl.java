/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import pl.edu.icm.unity.engine.server.HttpSessionsService;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Helper class, works as application singleton. Maintains an association of Unity's {@link LoginSession}s with
 * {@link HttpSession}s. The main purpose is to invalidate the latter when Unity session is expired. The implementation
 * also takes care about memory consumption: whenever a {@link HttpSession} is expired it is removed from the registry.
 * <p>
 * Thread safe.
 */
@Component
public class LoginToHttpSessionBinderImpl implements LoginToHttpSessionBinder
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, LoginToHttpSessionBinderImpl.class);

	private final HttpSessionsService httpSessionsService;
	private final Map<String, Collection<SelfHttpSessionWrappingAttribute>> bindings = new HashMap<>(1000);

	public LoginToHttpSessionBinderImpl(HttpSessionsService httpSessionsService)
	{
		this.httpSessionsService = httpSessionsService;
	}

	/**
	 * @param toRemove
	 * 		session id to remove
	 * @param soft
	 * 		if true then only the login data is removed from the HTTP session. Otherwise the whole session is
	 * 		invalidated
	 */
	@Override
	public synchronized void removeLoginSession(String toRemove, boolean soft)
	{
		Collection<SelfHttpSessionWrappingAttribute> httpSessions = bindings.remove(toRemove);
		if (httpSessions != null)
		{
			for (SelfHttpSessionWrappingAttribute sw : httpSessions)
			{
				if (!soft)
				{
					log.debug("Invalidating HTTP session {} of login session {}", sw.httpSessionId,
							sw.loginSessionId);
					httpSessionsService.invalidateSession(sw.httpSessionId);
				} else
				{
					log.debug("Unbinding login session {} from HTTP session {}", sw.loginSessionId,
							sw.httpSessionId);
					httpSessionsService.removeAttribute(sw.httpSessionId, USER_SESSION_KEY);
				}
			}
		}
	}

	@Override
	public synchronized void bindHttpSession(HttpSession session, LoginSession owning)
	{
		Collection<SelfHttpSessionWrappingAttribute> httpSessions = bindings.get(owning.getId());
		if (httpSessions == null)
		{
			httpSessions = new HashSet<>();
			bindings.put(owning.getId(), httpSessions);
		}
		log.debug("Binding HTTP session " + session.getId() + " to login session " + owning.getId());
		SelfHttpSessionWrappingAttribute wrapper = new SelfHttpSessionWrappingAttribute(session, owning.getId());
		httpSessions.add(wrapper);
		//to receive unbound event when the session is invalidated
		session.setAttribute(SELF_REFERENCING_ATTRIBUTE, wrapper);
		session.setAttribute(USER_SESSION_KEY, owning);
	}

	private synchronized void unbindHttpSession(SelfHttpSessionWrappingAttribute session, String owning)
	{
		Collection<SelfHttpSessionWrappingAttribute> httpSessions = bindings.get(owning);
		if (httpSessions != null)
			httpSessions.remove(session);
	}

	class SelfHttpSessionWrappingAttribute implements HttpSessionBindingListener
	{
		private final String loginSessionId;
		private final String httpSessionId;

		public SelfHttpSessionWrappingAttribute(HttpSession session, String loginSessionId)
		{
			this.loginSessionId = loginSessionId;
			this.httpSessionId = session.getId();
		}

		@Override
		public void valueUnbound(HttpSessionBindingEvent event)
		{
			log.debug("Value unbound for session {}", loginSessionId);
			unbindHttpSession(this, loginSessionId);
		}
	}
}
