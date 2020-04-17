/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageArea;
import pl.edu.icm.unity.engine.api.msg.MessageAreaProvider;

@Component
public class SamlMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "saml";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "SamlMessageAreaProvider.displayedName", true);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
