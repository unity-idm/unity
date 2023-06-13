/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageArea;
import pl.edu.icm.unity.base.message.MessageAreaProvider;

@Component
public class WebUIMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "webui";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "WebUIMessageAreaProvider.displayedName", true);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
