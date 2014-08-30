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
	
	private HashSet<Listener> listenerList;

	public SandboxAuthnRouterImpl()
	{
		listenerList = new LinkedHashSet<Listener>();
	}
	
	@Override
	public synchronized void fireEvent(SandboxAuthnEvent event) 
	{
		for (Listener listener : listenerList)
		{
			listener.handle(event);
		}
	}

	@Override
	public synchronized void addListener(Listener listener) 
	{
		listenerList.add(listener);
	}

	@Override
	public synchronized void removeListener(Listener listener) 
	{
		listenerList.remove(listener);
	}

}
