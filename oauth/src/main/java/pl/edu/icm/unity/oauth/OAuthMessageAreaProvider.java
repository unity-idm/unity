/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageArea;
import pl.edu.icm.unity.engine.api.msg.MessageAreaProvider;

@Component
public class OAuthMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "oauth";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "OAuthMessageAreaProvider.displayedName", true);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
