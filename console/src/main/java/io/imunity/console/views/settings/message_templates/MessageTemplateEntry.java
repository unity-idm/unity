/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.message_templates;

import pl.edu.icm.unity.base.msg_template.MessageTemplate;

class MessageTemplateEntry
{
	public final MessageTemplate messageTemplate;

	MessageTemplateEntry(MessageTemplate messageTemplate)
	{
		this.messageTemplate = messageTemplate;
	}

	public boolean anyFieldContains(String searched)
	{
		if(searched == null || searched.isBlank())
			return true;
		String textLower = searched.toLowerCase();

		if (messageTemplate.getName().toLowerCase().contains(textLower))
		{
			return true;
		}

		if (messageTemplate.getType().toString().toLowerCase().contains(textLower))
		{
			return true;
		}

		if (messageTemplate.getNotificationChannel() != null && messageTemplate.getNotificationChannel()
				.toString().toLowerCase().contains(textLower))
		{
			return true;
		}

		if (messageTemplate.getConsumer().toString().toLowerCase().contains(textLower))
		{
			return true;
		}

		return false;
	}

}
