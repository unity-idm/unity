/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;

/**
 * Java dynamic proxy builder, decorating wrapped objects with event generation. This is the same as 
 * {@link EventProxyBuilder}, but for other classes - we can't have all in one builder as then we would easily 
 * get circular dependencies.
 *   
 * @author K. Benedyczak
 */
public class EventProxyBuilderHL2
{
	private static final ClassLoader classLoader = EventProxyBuilderHL2.class.getClassLoader();
	
	@Autowired @Qualifier("plain")
	private MessageTemplateManagement msgTempMan;
	
	@Autowired
	private EventProcessor eventProcessor;
	
	public MessageTemplateManagement getMessageTemplateManagementInstance()
	{
		return (MessageTemplateManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {MessageTemplateManagement.class}, 
				new EventDecoratingHandler(msgTempMan, eventProcessor, 
						MessageTemplateManagement.class.getSimpleName()));
	}
	
}
