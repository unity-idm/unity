/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.ServerManagement;

/**
 * Java dynamic proxy builder, decorating wrapped objects with event generation.
 * @author K. Benedyczak
 */
public class EventProxyBuilder
{
	private static final ClassLoader classLoader = EventProxyBuilder.class.getClassLoader();
	
	@Autowired @Qualifier("plain")
	private AttributesManagement attributesManagement;
	@Autowired @Qualifier("plain")
	private GroupsManagement groupsMan;
	@Autowired @Qualifier("plain")
	private IdentitiesManagement idsMan;
	@Autowired @Qualifier("plain")
	private ServerManagement serverMan;
	@Autowired @Qualifier("plain")
	private EndpointManagement endpointMan;
	@Autowired @Qualifier("plain")
	private AuthenticationManagement authnMan;
	
	@Autowired
	private EventProcessor eventProcessor;

	
	public AttributesManagement getAttributeManagementInstance()
	{
		return (AttributesManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {AttributesManagement.class}, 
				new EventDecoratingHandler(attributesManagement, eventProcessor,  
						AttributesManagement.class.getSimpleName()));
	}

	public GroupsManagement getGroupsManagementInstance()
	{
		return (GroupsManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {GroupsManagement.class}, 
				new EventDecoratingHandler(groupsMan, eventProcessor, 
						GroupsManagement.class.getSimpleName()));
	}

	public IdentitiesManagement getIdentitiesManagementInstance()
	{
		return (IdentitiesManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {IdentitiesManagement.class}, 
				new EventDecoratingHandler(idsMan, eventProcessor, 
						IdentitiesManagement.class.getSimpleName()));
	}

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
}
