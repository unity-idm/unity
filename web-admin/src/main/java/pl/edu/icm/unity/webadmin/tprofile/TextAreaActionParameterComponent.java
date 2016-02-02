/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

import com.vaadin.ui.TextArea;

/**
 * Trivial, {@link TextArea} based implementation of {@link ActionParameterComponent}. 
 * @author K. Benedyczak
 */
public class TextAreaActionParameterComponent extends TextArea implements ActionParameterComponent
{
	public TextAreaActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		super(desc.getName() + ":");
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		setColumns(80);
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
