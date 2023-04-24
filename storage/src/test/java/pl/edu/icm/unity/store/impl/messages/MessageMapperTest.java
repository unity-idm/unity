/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.messages;

import java.util.Locale;
import java.util.function.Function;

import pl.edu.icm.unity.msg.Message;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class MessageMapperTest extends MapperTestBase<Message, DBMessage>
{

	@Override
	protected Message getFullAPIObject()
	{
		return new Message("name", new Locale("pl"), "value");
	}

	@Override
	protected DBMessage getFullDBObject()
	{

		return DBMessage.builder()
				.withLocale("pl")
				.withName("name")
				.withValue("value")
				.build();
	}

	@Override
	protected Pair<Function<Message, DBMessage>, Function<DBMessage, Message>> getMapper()
	{
		return Pair.of(MessageMapper::map, MessageMapper::map);
	}

}
