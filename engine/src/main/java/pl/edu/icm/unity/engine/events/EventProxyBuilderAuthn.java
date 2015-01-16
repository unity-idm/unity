/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.server.api.AuthenticationManagement;

/**
 * Java dynamic proxy builder, decorating wrapped objects with event generation: AuthnManagement
 *   
 * @author K. Benedyczak
 */
public class EventProxyBuilderAuthn
{
	private static final ClassLoader classLoader = EventProxyBuilderAuthn.class.getClassLoader();
	
	@Autowired @Qualifier("plain")
	private AuthenticationManagement authnMan;
	
	@Autowired
	private EventProcessor eventProcessor;

	
	public AuthenticationManagement getAuthenticationManagementInstance()
	{
		return (AuthenticationManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {AuthenticationManagement.class}, 
				new EventDecoratingHandler(authnMan, eventProcessor, 
						AuthenticationManagement.class.getSimpleName()));
	}
}
