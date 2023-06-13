/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageArea;
import pl.edu.icm.unity.base.message.MessageAreaProvider;

@Component
public class CompositePasswordMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "composite-password";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "CompositePasswordMessageAreaProvider.displayedName", false);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
