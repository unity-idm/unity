/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.msgtemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;

/**
 * Handles import/export of {@link MessageTemplate}.
 * @author K. Benedyczak
 */
@Component
public class MessageTemplateIE extends GenericObjectIEBase<MessageTemplate>
{
	@Autowired
	public MessageTemplateIE(MessageTemplateDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 105, 
				MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE);
	}
	
	@Override
	protected MessageTemplate convert(ObjectNode src)
	{
		return MessageTemplateMapper.map(jsonMapper.convertValue(src, DBMessageTemplate.class));
	}

	@Override
	protected ObjectNode convert(MessageTemplate src)
	{
		return jsonMapper.convertValue(MessageTemplateMapper.map(src), ObjectNode.class);
	}
}



