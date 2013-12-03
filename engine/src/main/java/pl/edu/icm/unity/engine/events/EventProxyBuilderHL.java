/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.server.api.TranslationProfileManagement;

/**
 * Java dynamic proxy builder, decorating wrapped objects with event generation. This is the same as 
 * {@link EventProxyBuilder}, but for other classes - we can't have all in one builder as then we would easily 
 * get circular dependencies.
 *   
 * @author K. Benedyczak
 */
public class EventProxyBuilderHL
{
	private static final ClassLoader classLoader = EventProxyBuilderHL.class.getClassLoader();
	
	@Autowired @Qualifier("plain")
	private TranslationProfileManagement tprofileMan;
	
	@Autowired
	private EventProcessor eventProcessor;

	
	public TranslationProfileManagement getTranslationProfileManagementInstance()
	{
		return (TranslationProfileManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {TranslationProfileManagement.class}, 
				new EventDecoratingHandler(tprofileMan, eventProcessor, 
						TranslationProfileManagement.class.getSimpleName()));
	}
}
