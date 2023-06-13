/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.tprofile;

import com.vaadin.data.Binder;
import com.vaadin.ui.TextArea;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

/**
 * Trivial, {@link TextArea} based implementation of {@link ActionParameterComponent}. 
 * @author K. Benedyczak
 */
public class TextAreaActionParameterComponent extends TextArea implements ActionParameterComponent
{
	private Binder<StringValueBean> binder;
	
	public TextAreaActionParameterComponent(ActionParameterDefinition desc, MessageSource msg)
	{
		super(desc.getName() + ":");
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		setWidth(70, Unit.PERCENTAGE);
		binder = new Binder<>(StringValueBean.class);
		if (desc.isMandatory())
		{
			binder.forField(this).asRequired(msg.getMessage("fieldRequired")).bind("value");

		} else
		{
			binder.forField(this).bind("value");
		}
		binder.setBean(new StringValueBean());		
	}
	
	@Override
	public String getActionValue()
	{
		return binder.getBean().getValue();
	}

	@Override
	public void setActionValue(String value)
	{
		binder.setBean(new StringValueBean(value));
	}

	@Override
	public void addValueChangeCallback(Runnable callback)
	{
		binder.addValueChangeListener((e) -> { callback.run(); });		
	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}
}
