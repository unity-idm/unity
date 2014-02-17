/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.msgtemplates.MessageTemplateConsumer;

/**
 * Maintains a {@link MessageTemplateConsumer}s.
 * 
 * @author P. Piernik
 */
@Component
public class MessageTemplateConsumersRegistry extends TypesRegistryBase<MessageTemplateConsumer>
{

	@Autowired
	public MessageTemplateConsumersRegistry(List<MessageTemplateConsumer> typeElements)
	{
		super(typeElements);
	}
	
	@Override
	protected String getId(MessageTemplateConsumer from)
	{
		return from.getName();
	}

}
