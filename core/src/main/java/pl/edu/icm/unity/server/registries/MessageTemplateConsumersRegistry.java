/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;

/**
 * Maintains a {@link MessageTemplateDefinition}s.
 * 
 * @author P. Piernik
 */
@Component
public class MessageTemplateConsumersRegistry extends TypesRegistryBase<MessageTemplateDefinition>
{

	@Autowired
	public MessageTemplateConsumersRegistry(List<MessageTemplateDefinition> typeElements)
	{
		super(typeElements);
	}
	
	@Override
	protected String getId(MessageTemplateDefinition from)
	{
		return from.getName();
	}

}
