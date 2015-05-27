/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;

import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.VaadinService;

/**
 * Simple implementation of {@link SandboxAuthnRouter} interface, used by
 * {@link SandboxUI} to dispatch authn events. 
 * 
 * @author R. Krysinski
 */
public class SandboxAuthnRouterImpl implements SandboxAuthnRouter 
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, SandboxAuthnRouterImpl.class);
	private Map<String, AuthnResultListener> authnListenerList;

	public SandboxAuthnRouterImpl()
	{
		authnListenerList = new HashMap<>();
	}
	
	@Override
	public void fireEvent(SandboxAuthnEvent event) 
	{
		synchronized (authnListenerList)
		{
			for (AuthnResultListener listener : authnListenerList.values())
			{
				listener.handle(event);
			}
		}
	}
	
	@Override
	public void addListener(AuthnResultListener listener) 
	{
		final String sessionId = VaadinService.getCurrentRequest().getWrappedSession().getId();
		LOG.debug("Adding AuthnResultListener: " + sessionId);
		synchronized (authnListenerList)
		{
			authnListenerList.put(sessionId, listener);
		}
		final VaadinService vaadinService = VaadinService.getCurrent();
		vaadinService.addSessionDestroyListener(new SessionDestroyListener() 
		{
			@Override
			public void sessionDestroy(SessionDestroyEvent event) 
			{
				LOG.debug("removing AuthnResultListener: " + sessionId);
				synchronized (authnListenerList)
				{
					authnListenerList.remove(sessionId);
				}				
				vaadinService.removeSessionDestroyListener(this);
			}
		}); 	
	}
}
