/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.WeakHashMap;

import org.springframework.stereotype.Component;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

/**
 * Simple implementation of {@link SandboxAuthnRouter} interface, used by
 * {@link SandboxUI} to dispatch authn events. 
 * 
 * @author R. Krysinski
 */
@Component
public class SandboxAuthnRouterImpl implements SandboxAuthnRouter 
{
	
	private WeakHashMap<WrappedSession, RemoteAuthnInputListener> inputListenerList;
	private WeakHashMap<WrappedSession, AuthnResultListener> authnListenerList;

	public SandboxAuthnRouterImpl()
	{
		inputListenerList = new WeakHashMap<WrappedSession, RemoteAuthnInputListener>();
		authnListenerList = new WeakHashMap<WrappedSession, AuthnResultListener>();
	}
	
	@Override
	public void fireEvent(SandboxRemoteAuthnInputEvent event) 
	{
		synchronized (inputListenerList)
		{
			for (RemoteAuthnInputListener listener : inputListenerList.values())
			{
				listener.handle(event);
			}
		}
	}

	@Override
	public void fireEvent(SandboxAuthnResultEvent event) 
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
	public void addListener(RemoteAuthnInputListener listener) 
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		synchronized (inputListenerList)
		{
			inputListenerList.put(session, listener);
		}
	}

	@Override
	public void addListener(AuthnResultListener listener) 
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		synchronized (authnListenerList)
		{
			authnListenerList.put(session, listener);
		}
	}
}
