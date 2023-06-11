/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.msgtemplate;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.i18n.I18nMessage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.base.msgtemplates.MessageType;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;

public class MsgTemplateTest extends AbstractNamedWithTSTest<MessageTemplate>
{
	@Autowired
	private MessageTemplateDB dao;
	
	@Override
	protected NamedCRUDDAOWithTS<MessageTemplate> getDAO()
	{
		return dao;
	}

	@Override
	protected MessageTemplate getObject(String id)
	{
		return new MessageTemplate(id, "description",
				new I18nMessage(new I18nString("s"), new I18nString("b")),
				"consumer", MessageType.PLAIN, "default_email");
	}

	@Override
	protected MessageTemplate mutateObject(MessageTemplate src)
	{
		src.setName("name-Changed");
		src.setDescription("description2");
		src.setMessage(new I18nMessage(new I18nString("s2"), new I18nString("b2")));
		src.setType(MessageType.HTML);
		return src;
	}
}
