/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.msgtemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.basic.MessageTemplate;

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
		super(dao, jsonMapper, MessageTemplate.class, 105, "messageTemplates");
	}
}



