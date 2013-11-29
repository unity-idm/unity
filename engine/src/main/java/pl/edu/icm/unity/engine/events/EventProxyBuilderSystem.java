/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;

/**
 * Java dynamic proxy builder, decorating wrapped objects with event generation. This is the same as 
 * {@link EventProxyBuilder}, but for other classes - we can't have all in one builder as then we would easily 
 * get circular dependencies.
 *   
 * @author K. Benedyczak
 */
public class EventProxyBuilderSystem
{
	private static final ClassLoader classLoader = EventProxyBuilderSystem.class.getClassLoader();
	
	@Autowired @Qualifier("plain")
	private ServerManagement serverMan;
	@Autowired @Qualifier("plain")
	private EndpointManagement endpointMan;
	@Autowired @Qualifier("plain")
	private AuthenticationManagement authnMan;
	@Autowired @Qualifier("plain")
	private NotificationsManagement notMan;
	@Autowired @Qualifier("plain")
	private TranslationProfileManagement tprofileMan;
	
	@Autowired
	private EventProcessor eventProcessor;

	
	public ServerManagement getServerManagementInstance()
	{
		return (ServerManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {ServerManagement.class}, 
				new EventDecoratingHandler(serverMan, eventProcessor, 
						ServerManagement.class.getSimpleName()));
	}

	public EndpointManagement getEndpointsManagementInstance()
	{
		return (EndpointManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {EndpointManagement.class}, 
				new EventDecoratingHandler(endpointMan, eventProcessor, 
						EndpointManagement.class.getSimpleName()));
	}

	public AuthenticationManagement getAuthenticationManagementInstance()
	{
		return (AuthenticationManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {AuthenticationManagement.class}, 
				new EventDecoratingHandler(authnMan, eventProcessor, 
						AuthenticationManagement.class.getSimpleName()));
	}

	public NotificationsManagement getNotificationsManagementInstance()
	{
		return (NotificationsManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {NotificationsManagement.class}, 
				new EventDecoratingHandler(notMan, eventProcessor, 
						NotificationsManagement.class.getSimpleName()));
	}
	
	public TranslationProfileManagement getTranslationProfileManagementInstance()
	{
		return (TranslationProfileManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {TranslationProfileManagement.class}, 
				new EventDecoratingHandler(tprofileMan, eventProcessor, 
						TranslationProfileManagement.class.getSimpleName()));
	}
}
