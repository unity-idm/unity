/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.tprofile;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

/**
 * {@link ComboBox} based editor of enum paramter.
 * @author K. Benedyczak
 */
public class EnumActionParameterComponent extends BaseEnumActionParameterComponent implements ActionParameterComponent
{
	public EnumActionParameterComponent(ActionParameterDefinition desc, MessageSource msg)
	{
		super(desc, msg, desc.getEnumClass().getEnumConstants());
	}
}
