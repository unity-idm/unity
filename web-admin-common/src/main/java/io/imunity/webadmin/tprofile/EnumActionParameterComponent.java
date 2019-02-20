/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * {@link ComboBox} based editor of enum paramter.
 * @author K. Benedyczak
 */
public class EnumActionParameterComponent extends BaseEnumActionParameterComponent implements ActionParameterComponent
{
	public EnumActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		super(desc, msg, desc.getEnumClass().getEnumConstants());
	}
}
