/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

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

	@Override
	public void addValueChangeCallback(ActionParameterValueChangeCallback callback)
	{
		setImmediate(true);
		addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
			{
				callback.refresh();
				
			}
		});
		
	}
	
	
}
