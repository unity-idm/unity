/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.store.impl.messages;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.message.Message;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.api.MessagesDAO;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;

public class MessageTest extends AbstractBasicDAOTest<Message>
{

	@Autowired
	private MessagesDAO dao;
	
	@Override
	protected BasicCRUDDAO<Message> getDAO()
	{
		return dao;
	}

	@Override
	protected Message getObject(String id)
	{
		return new Message(id, Locale.ENGLISH, "test val");
	}

	@Override
	protected Message mutateObject(Message src)
	{
		return new Message(src.getName(), Locale.ENGLISH, "test val2");
	}

}
