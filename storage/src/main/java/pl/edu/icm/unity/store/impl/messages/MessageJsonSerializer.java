/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.messages;

import java.util.Locale;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.message.Message;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

/**
 * Serializes {@link Message} to/from RDBMS {@link MessageBean} form.
 * 
 * @author P.Piernik
 */
@Component
class MessageJsonSerializer implements RDBMSObjectSerializer<Message, MessageBean>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, MessageJsonSerializer.class);

	@Autowired
	private ObjectMapper jsonMapper;
	
	@Override
	public MessageBean toDB(Message from)
	{
		return new MessageBean(from.getName(), from.getLocale().toString(), from.getValue().getBytes());
	}

	@Override
	public Message fromDB(MessageBean bean)
	{
		return new Message(bean.getName(), Locale.forLanguageTag(bean.getLocale()), new String(bean.getContents()));
	}

	ObjectNode toJson(Message exportedObj)
	{
		return jsonMapper.valueToTree(MessageMapper.map(exportedObj));
	}

	Message fromJson(ObjectNode src)
	{
		try
		{
			return MessageMapper.map(jsonMapper.treeToValue(src, DBMessage.class));
		} catch (JsonProcessingException e)
		{
			log.error("Failed to deserialize StoredMessage object:", e);
		}
		return null;
	}
}
