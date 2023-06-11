/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.msgtemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link MessageTemplate}.
 * 
 * @author P. Piernik
 */
@Component
public class MessageTemplateHandler extends DefaultEntityHandler<MessageTemplate>
{
	public static final String MESSAGE_TEMPLATE_OBJECT_TYPE = "messageTemplate";

	@Autowired
	public MessageTemplateHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, MESSAGE_TEMPLATE_OBJECT_TYPE, MessageTemplate.class);
	}

	@Override
	public GenericObjectBean toBlob(MessageTemplate value)
	{
		try
		{
			return new GenericObjectBean(value.getName(),
					jsonMapper.writeValueAsBytes(MessageTemplateMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize message template to JSON", e);
		}
	}

	@Override
	public MessageTemplate fromBlob(GenericObjectBean blob)
	{
		try
		{
			return MessageTemplateMapper.map(jsonMapper.readValue(blob.getContents(), DBMessageTemplate.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize message template from JSON", e);
		}
	}
}
