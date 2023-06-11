/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageArea;
import pl.edu.icm.unity.base.message.MessageAreaProvider;

@Component
public class StdextMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "stdext";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "StdextMessageAreaProvider.displayedName", true);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
