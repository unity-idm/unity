/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.RealmsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;

/**
 * Java dynamic proxy builder, decorating wrapped objects with event generation. 
 * See also {@link EventProxyBuilderSystem} which does the same task for the rest of engine classes.
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
	private PreferencesManagement prefMan;
	@Autowired @Qualifier("plain")
	private RegistrationsManagement regMan;
	@Autowired @Qualifier("plain")
	private RealmsManagement realmMan;
	
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

	public PreferencesManagement getPreferencesManagementInstance()
	{
		return (PreferencesManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {PreferencesManagement.class}, 
				new EventDecoratingHandler(prefMan, eventProcessor, 
						PreferencesManagement.class.getSimpleName()));
	}
	
	public RegistrationsManagement getRegistrationsManagementInstance()
	{
		return (RegistrationsManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {RegistrationsManagement.class}, 
				new EventDecoratingHandler(regMan, eventProcessor, 
						RegistrationsManagement.class.getSimpleName()));
	}

	public RealmsManagement getRealmsManagementInstance()
	{
		return (RealmsManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {RealmsManagement.class}, 
				new EventDecoratingHandler(realmMan, eventProcessor, 
						RealmsManagement.class.getSimpleName()));
	}
}
