/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.msgtemplate;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.objstore.AbstractObjStoreTest;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;

public class MsgTemplateTest extends AbstractObjStoreTest<MessageTemplate>
{
	@Autowired
	private MessageTemplateDB dao;
	
	@Override
	protected GenericObjectsDAO<MessageTemplate> getDAO()
	{
		return dao;
	}

	@Override
	protected MessageTemplate getObject(String id)
	{
		return new MessageTemplate(id, "description",
				new I18nMessage(new I18nString("s"), new I18nString("b")),
				"consumer");
	}

	@Override
	protected MessageTemplate mutateObject(MessageTemplate src)
	{
		src.setName("name-Changed");
		src.setDescription("description2");
		src.setMessage(new I18nMessage(new I18nString("s2"), new I18nString("b2")));
		return src;
	}
}
