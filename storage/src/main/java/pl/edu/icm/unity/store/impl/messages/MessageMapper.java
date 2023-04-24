/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.messages;

import java.util.Locale;

import pl.edu.icm.unity.msg.Message;

class MessageMapper
{
	static DBMessage map(Message message)
	{
		return DBMessage.builder()
				.withName(message.getName())
				.withValue(message.getValue())
				.withLocale(message.getLocale()
						.toString())
				.build();

	}

	static Message map(DBMessage dbMessage)
	{
		return new Message(dbMessage.name, new Locale(dbMessage.locale), dbMessage.value);
	}
}
