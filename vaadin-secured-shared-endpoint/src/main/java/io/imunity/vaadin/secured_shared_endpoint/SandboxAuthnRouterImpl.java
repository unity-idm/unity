/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.secured_shared_endpoint;

import com.vaadin.flow.server.VaadinService;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.synchronizedSet;

class SandboxAuthnRouterImpl implements SandboxAuthnRouter
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, SandboxAuthnRouterImpl.class);
	private final Set<AuthnResultListener> authnListenerList;

	public SandboxAuthnRouterImpl()
	{
		authnListenerList = synchronizedSet(new HashSet<>());
	}

	@Override
	public void fireEvent(SandboxAuthnEvent event) 
	{
		LOG.debug("Fire event: {}", event);
		synchronized (authnListenerList)
		{
				for (AuthnResultListener listener: authnListenerList)
					listener.onSandboxAuthnResult(event);
		}
	}

	@Override
	public void addListener(AuthnResultListener listener) 
	{
		String sessionId = VaadinService.getCurrentRequest().getWrappedSession().getId();
		LOG.debug("Adding AuthnResultListener: " + sessionId);
		authnListenerList.add(listener);
	}
	
	@Override
	public void removeListener(AuthnResultListener listener)
	{
		authnListenerList.remove(listener);
	}
}
