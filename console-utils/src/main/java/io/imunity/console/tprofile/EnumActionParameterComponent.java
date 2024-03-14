/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

public class EnumActionParameterComponent extends BaseEnumActionParameterComponent implements ActionParameterComponent
{
	public EnumActionParameterComponent(ActionParameterDefinition desc, MessageSource msg)
	{
		super(desc, msg, desc.getEnumClass().getEnumConstants());
	}
}
