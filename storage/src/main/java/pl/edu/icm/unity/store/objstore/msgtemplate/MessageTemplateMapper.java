/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.msgtemplate;

import java.util.Optional;

import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.MessageType;

class MessageTemplateMapper
{
	static DBMessageTemplate map(MessageTemplate messageTemplate)
	{
		return DBMessageTemplate.builder()
				.withConsumer(messageTemplate.getConsumer())
				.withDescription(messageTemplate.getDescription())
				.withName(messageTemplate.getName())
				.withNotificationChannel(messageTemplate.getNotificationChannel())
				.withType(messageTemplate.getType()
						.name())
				.withMessage(Optional.ofNullable(messageTemplate.getMessage())
						.map(I18nMessageMapper::map)
						.orElse(null))

				.build();
	}

	static MessageTemplate map(DBMessageTemplate dbMessageTemplate)
	{
		return new MessageTemplate(dbMessageTemplate.name, dbMessageTemplate.description,
				Optional.ofNullable(dbMessageTemplate.message)
						.map(I18nMessageMapper::map)
						.orElse(null),
				dbMessageTemplate.consumer, MessageType.valueOf(dbMessageTemplate.type),
				dbMessageTemplate.notificationChannel);
	}
}
