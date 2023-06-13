/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.binding.ToggleWithDefault;

/**
 * Combo box allowing to choose Enable/Disable/Default value 
 * 
 * @author P.Piernik
 *
 */
public class EnableDisableCombo extends EnumComboBox<ToggleWithDefault>
{
	public EnableDisableCombo(String caption, MessageSource msg)
	{
		super(caption, msg, "EnableDisableCombo.", ToggleWithDefault.class, ToggleWithDefault.bydefault);
	}
}
