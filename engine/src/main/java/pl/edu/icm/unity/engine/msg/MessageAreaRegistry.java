/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.msg;

import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.msg.MessageAreaProvider;

@Component
public class MessageAreaRegistry extends TypesRegistryBase<MessageAreaProvider>
{
	public MessageAreaRegistry(List<? extends MessageAreaProvider> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(MessageAreaProvider from)
	{
		return from.getName();
	}
}
