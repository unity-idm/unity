/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.messages;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.msg.Message;
import pl.edu.icm.unity.store.api.MessagesDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;

/**
 * Handles import/export of messages.
 * 
 * @author P.Piernik
 */
@Component
public class MessageIE extends AbstractIEBase<Message>
{
	public static final String MESSAGES_OBJECT_TYPE = "messages";

	private final MessagesDAO dao;
	private final MessageJsonSerializer serializer;

	@Autowired
	public MessageIE(MessagesDAO dao, MessageJsonSerializer serializer)
	{
		super(11, MESSAGES_OBJECT_TYPE);
		this.dao = dao;
		this.serializer = serializer;
	}

	@Override
	protected List<Message> getAllToExport()
	{
		return dao.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(Message exportedObj)
	{
		return serializer.toJson(exportedObj);
	}

	@Override
	protected void createSingle(Message toCreate)
	{
		dao.create(toCreate);
	}

	@Override
	protected Message fromJsonSingle(ObjectNode src)
	{
		return serializer.fromJson(src);
	}
}
