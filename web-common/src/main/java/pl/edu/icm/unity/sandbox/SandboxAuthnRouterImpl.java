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
 * {@link SandboxUI} to dispatcher authn events. 
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
		//TODO: thread safe LinkedHashSet?
		inputListenerList = new LinkedHashSet<RemoteAuthnInputListener>();
		authnListenerList = new LinkedHashSet<AuthnResultListener>();
	}
	
	@Override
	public synchronized void fireEvent(SandboxRemoteAuthnInputEvent event) 
	{
		for (RemoteAuthnInputListener listener : inputListenerList)
		{
			listener.handle(event);
		}
	}

	@Override
	public void fireEvent(SandboxAuthnResultEvent event) 
	{
		for (AuthnResultListener listener : authnListenerList)
		{
			listener.handle(event);
		}
	}
	
	@Override
	public synchronized void addListener(RemoteAuthnInputListener listener) 
	{
		inputListenerList.add(listener);
	}

	@Override
	public synchronized void removeListener(RemoteAuthnInputListener listener) 
	{
		inputListenerList.remove(listener);
	}

	@Override
	public void addListener(AuthnResultListener listener) 
	{
		authnListenerList.add(listener);
	}

	@Override
	public void removeListener(AuthnResultListener listener) 
	{
		authnListenerList.remove(listener);
		
	}
}
