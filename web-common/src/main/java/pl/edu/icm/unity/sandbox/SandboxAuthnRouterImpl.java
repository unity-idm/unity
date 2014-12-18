/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

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
@Component
public class SandboxAuthnRouterImpl implements SandboxAuthnRouter 
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, SandboxAuthnRouterImpl.class);
	private Map<String, AuthnResultListener> authnListenerList;

	public SandboxAuthnRouterImpl()
	{
		authnListenerList = new HashMap<String, AuthnResultListener>();
		if (LOG.isDebugEnabled()) 
		{
			Thread thread = new Thread(new Runnable() 
			{
				@Override
				public void run() 
				{
					while (true)
					{
						int authnSize = 0;
						synchronized (authnListenerList)
						{
							authnSize = authnListenerList.size();
						}						
						if (authnSize > 0)
							LOG.debug("authnListenerList.size()==" + authnSize);
						try 
						{
							Thread.sleep(5000);
						} catch (InterruptedException e) 
						{
							break;
						}
					}
				}
			});
			thread.setName("SandboxAuthnRouterImpl");
			thread.start();
		}
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
		LOG.debug("addin AuthnResultListener: " + sessionId);
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
