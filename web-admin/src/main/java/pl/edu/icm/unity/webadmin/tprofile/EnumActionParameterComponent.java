/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.ComboBox;

/**
 * {@link ComboBox} based editor of enum paramter.
 * @author K. Benedyczak
 */
public class EnumActionParameterComponent extends BaseEnumActionParameterComponent implements ActionParameterComponent
{
	public EnumActionParameterComponent(ActionParameterDesc desc, UnityMessageSource msg)
	{
		super(desc, msg, desc.getEnumClass().getEnumConstants());
	}
}
