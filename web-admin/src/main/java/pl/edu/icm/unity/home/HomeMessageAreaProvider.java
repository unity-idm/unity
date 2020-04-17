/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.home;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageArea;
import pl.edu.icm.unity.engine.api.msg.MessageAreaProvider;

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
