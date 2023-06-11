/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ws;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageArea;
import pl.edu.icm.unity.base.message.MessageAreaProvider;

@Component
public class WsMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "ws";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "WsMessageAreaProvider.displayedName", false);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
