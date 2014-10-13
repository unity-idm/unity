/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.HashSet;
import java.util.LinkedHashSet;

import org.springframework.stereotype.Component;

/**
 * Simple implementation of {@link SandboxAuthnRouter} interface, used by
 * {@link SandboxUI} to dispatch authn events. 
 * 
 * @author R. Krysinski
 */
@Component
public class SandboxAuthnRouterImpl implements SandboxAuthnRouter 
{
	
	private HashSet<RemoteAuthnInputListener> inputListenerList;
	private HashSet<AuthnResultListener> authnListenerList;

	public SandboxAuthnRouterImpl()
	{
		inputListenerList = new LinkedHashSet<RemoteAuthnInputListener>();
		authnListenerList = new LinkedHashSet<AuthnResultListener>();
	}
	
	@Override
	public void fireEvent(SandboxRemoteAuthnInputEvent event) 
	{
		synchronized (inputListenerList)
		{
			for (RemoteAuthnInputListener listener : inputListenerList)
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
			for (AuthnResultListener listener : authnListenerList)
			{
				listener.handle(event);
			}
		}
	}
	
	@Override
	public void addListener(RemoteAuthnInputListener listener) 
	{
		synchronized (inputListenerList)
		{
			inputListenerList.add(listener);
		}
	}

	@Override
	public void removeListener(RemoteAuthnInputListener listener) 
	{
		synchronized (inputListenerList)
		{
			inputListenerList.remove(listener);
		}
	}

	@Override
	public void addListener(AuthnResultListener listener) 
	{
		synchronized (authnListenerList)
		{
			authnListenerList.add(listener);
		}
	}

	@Override
	public void removeListener(AuthnResultListener listener) 
	{
		synchronized (authnListenerList)
		{
			authnListenerList.remove(listener);
		}
		
	}
}
