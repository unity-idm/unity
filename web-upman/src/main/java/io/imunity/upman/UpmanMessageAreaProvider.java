/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageArea;
import pl.edu.icm.unity.msg.MessageAreaProvider;

@Component
public class UpmanMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "upman";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "UpmanMessageAreaProvider.displayedName", false);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
