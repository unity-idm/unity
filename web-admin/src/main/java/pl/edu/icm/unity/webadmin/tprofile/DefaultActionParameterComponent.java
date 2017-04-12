/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.ui.TextField;

/**
 * Trivial, {@link TextField} based implementation of {@link ActionParameterComponent}. 
 * @author K. Benedyczak
 */
public class DefaultActionParameterComponent extends RequiredTextField implements ActionParameterComponent
{
	public DefaultActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		super(desc.getName() + ":", msg);
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		setColumns(Styles.WIDE_TEXT_FIELD);
	}
	
	public DefaultActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg, boolean required)
	{
		this(desc, msg);
		setRequired(required);
	}
	
	@Override
	public String getActionValue()
	{
		return getValue();
	}

	@Override
	public void setActionValue(String value)
	{
		setValue(value);
	}
}
