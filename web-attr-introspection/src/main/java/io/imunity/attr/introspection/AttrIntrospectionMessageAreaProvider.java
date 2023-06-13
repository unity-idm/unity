/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageArea;
import pl.edu.icm.unity.base.message.MessageAreaProvider;

@Component
public class AttrIntrospectionMessageAreaProvider implements MessageAreaProvider
{
	public final String NAME = "attr-introspection";

	@Override
	public MessageArea getMessageArea()
	{
		return new MessageArea(NAME, "AttrIntrospectionMessageAreaProvider.displayedName", false);
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
