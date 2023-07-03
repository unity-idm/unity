/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.msgTemplates;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

/**
 * 
 * @author P.Piernik
 *
 */
class MessageTemplateEntry implements FilterableEntry
{
	public final MessageTemplate messageTemplate;

	MessageTemplateEntry(MessageTemplate messageTemplate)
	{
		this.messageTemplate = messageTemplate;
	}

	@Override
	public boolean anyFieldContains(String searched, MessageSource msg)
	{
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
