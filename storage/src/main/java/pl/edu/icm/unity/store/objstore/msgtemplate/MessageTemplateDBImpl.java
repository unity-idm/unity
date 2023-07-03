/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.msgtemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;

/**
 * Easy to use interface to {@link MessageTemplate} storage.
 *  
 * @author P. Piernik
 */
@Component
public class MessageTemplateDBImpl extends GenericObjectsDAOImpl<MessageTemplate> implements MessageTemplateDB
{

	@Autowired
	public MessageTemplateDBImpl(MessageTemplateHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, MessageTemplate.class, "message template");
	}
}

