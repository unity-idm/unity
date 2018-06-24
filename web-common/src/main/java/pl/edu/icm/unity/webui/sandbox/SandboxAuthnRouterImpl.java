/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.Registration;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;

/**
 * Simple implementation of {@link SandboxAuthnRouter} interface, used by
 * {@link SandboxUIBase} to dispatch authn events. 
 * 
 * @author R. Krysinski
 */
public class SandboxAuthnRouterImpl implements SandboxAuthnRouter 
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, SandboxAuthnRouterImpl.class);
	private Map<String, List<AuthnResultListener>> authnListenerList;

	public SandboxAuthnRouterImpl()
	{
		authnListenerList = new HashMap<>();
	}
	
	@Override
	public void firePartialEvent(SandboxAuthnEvent event) 
	{
		synchronized (authnListenerList)
		{
			for (Collection<AuthnResultListener> listeners : authnListenerList.values())
			{
				for (AuthnResultListener listener: listeners)
					listener.onPartialAuthnResult(event);
			}
		}
	}

	@Override
	public void fireCompleteEvent(AuthenticatedEntity entity) 
	{
		synchronized (authnListenerList)
		{
			for (Collection<AuthnResultListener> listeners : authnListenerList.values())
			{
				for (AuthnResultListener listener: listeners)
					listener.onCompleteAuthnResult(entity);
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
			List<AuthnResultListener> list = authnListenerList.get(sessionId);
			if (list == null)
			{
				list = new ArrayList<>();
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
		Registration listenerRegistration = vaadinService.addSessionDestroyListener(listener);
		listener.registration = listenerRegistration;
	}
	
	private class RemoveFromAuthnList implements SessionDestroyListener
	{
		private Registration registration;
		private String sessionId;
		
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
		final String sessionId = VaadinService.getCurrentRequest().getWrappedSession().getId();
		LOG.debug("Removing AuthnResultListener: " + sessionId);
		synchronized (authnListenerList)
		{
			List<AuthnResultListener> list = authnListenerList.get(sessionId);
			if (list != null)
			{
				list.remove(listener);
				if (list.isEmpty())
					authnListenerList.remove(sessionId);
			}
		}		
	}
}
