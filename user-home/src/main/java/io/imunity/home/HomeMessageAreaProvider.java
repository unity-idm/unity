/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageArea;
import pl.edu.icm.unity.base.message.MessageAreaProvider;

@Component
public class HomeMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "webhome";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "WebhomeMessageAreaProvider.displayedName", true);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
