/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console_utils.tprofile;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.translation.form.DynamicGroupParam;

public class DynamicGroupParamWithLabel extends DynamicGroupParam
{
	public final String label;

	public DynamicGroupParamWithLabel(String label, int index)
	{
		super(index);
		this.label = label;

	}

	public String getLabel(MessageSource msg)
	{
		return msg.getMessage("RegistrationFormEditor.dynamicGroup", label);
	}

}
