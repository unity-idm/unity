/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageArea;
import pl.edu.icm.unity.msg.MessageAreaProvider;

@Component
public class WebAdminMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "webadmin";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "WebAdminMessageAreaProvider.displayedName", false);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
