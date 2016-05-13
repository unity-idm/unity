/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.msgtemplate;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

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
		super(jsonMapper,  MESSAGE_TEMPLATE_OBJECT_TYPE, MessageTemplate.class);
	}

	@Override
	public GenericObjectBean toBlob(MessageTemplate value)
	{
		String json = value.toJson(jsonMapper);
		return new GenericObjectBean(value.getName(), json.getBytes(StandardCharsets.UTF_8), supportedType);
	}

	@Override
	public MessageTemplate fromBlob(GenericObjectBean blob)
	{
		return new MessageTemplate(new String(blob.getContents(), StandardCharsets.UTF_8), 
				jsonMapper);
	}
}
