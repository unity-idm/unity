/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.secured.shared.endpoint;

import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;

import java.util.*;

import static java.util.Collections.synchronizedSet;

class SandboxAuthnRouterImplV23 implements SandboxAuthnRouter
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, SandboxAuthnRouterImplV23.class);
	private final Map<String, Set<AuthnResultListener>> authnListenerList;

	public SandboxAuthnRouterImplV23()
	{
		authnListenerList = new HashMap<>();
	}

	@Override
	public void fireEvent(SandboxAuthnEvent event) 
	{
		LOG.debug("Fire event: {}", event);
		synchronized (authnListenerList)
		{
			for (Collection<AuthnResultListener> listeners : authnListenerList.values())
			{
				for (AuthnResultListener listener: listeners)
					listener.onSandboxAuthnResult(event);
			}
		}
	}

	@Override
	public void addListener(AuthnResultListener listener) 
	{
		final String sessionId = VaadinService.getCurrentRequest().getWrappedSession().getId();
		LOG.debug("Adding AuthnResultListener: " + sessionId);
		boolean first = false;
		synchronized (authnListenerList)
		{
			Set<AuthnResultListener> list = authnListenerList.get(sessionId);
			if (list == null)
			{
				list = synchronizedSet(new HashSet<>());
				authnListenerList.put(sessionId, list);
				first = true;
			}
			list.add(listener);
		}
		
		if (first)
			addCleanupTaskToSessionDestroy(sessionId);
	}

	private void addCleanupTaskToSessionDestroy(final String sessionId)
	{
		final VaadinService vaadinService = VaadinService.getCurrent();
		RemoveFromAuthnList listener = new RemoveFromAuthnList(sessionId);
		listener.registration = vaadinService.addSessionDestroyListener(listener);
	}
	
	private class RemoveFromAuthnList implements SessionDestroyListener
	{
		private Registration registration;
		private final String sessionId;
		
		public RemoveFromAuthnList(String sessionId)
		{
			this.sessionId = sessionId;
		}

		@Override
		public void sessionDestroy(SessionDestroyEvent event)
		{
			synchronized (authnListenerList)
			{
				authnListenerList.remove(sessionId);
			}
			if (registration != null)
				registration.remove();
		}
	}
	
	@Override
	public void removeListener(AuthnResultListener listener)
	{
		synchronized (authnListenerList)
		{
			authnListenerList.entrySet().stream()
					.filter(entry -> entry.getValue().contains(listener))
					.findAny()
					.map(Map.Entry::getKey)
					.ifPresent(key -> {
						authnListenerList.get(key).remove(listener);
						if(authnListenerList.get(key).isEmpty())
							authnListenerList.remove(key);
					});
		}		
	}
}
