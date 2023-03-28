/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.msgtemplate;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.DBI18nString;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;

public class MessageTemplateMapperTest extends MapperTestBase<MessageTemplate, DBMessageTemplate>
{

	@Override
	protected MessageTemplate getFullAPIObject()
	{

		return new MessageTemplate("name", "desc", new I18nMessage(new I18nString("sub"), new I18nString("body")),
				"consumer", MessageType.PLAIN, "channel");

	}

	@Override
	protected DBMessageTemplate getFullDBObject()
	{

		return DBMessageTemplate.builder()
				.withName("name")
				.withConsumer("consumer")
				.withType("PLAIN")
				.withDescription("desc")
				.withNotificationChannel("channel")
				.withMessage(DBI18nMessage.builder()
						.withBody(DBI18nString.builder()
								.withDefaultValue("body")
								.build())
						.withSubject(DBI18nString.builder()
								.withDefaultValue("sub")
								.build())
						.build())
				.build();
	}

	@Override
	protected Pair<Function<MessageTemplate, DBMessageTemplate>, Function<DBMessageTemplate, MessageTemplate>> getMapper()
	{
		return Pair.of(MessageTemplateMapper::map, MessageTemplateMapper::map);
	}

}
