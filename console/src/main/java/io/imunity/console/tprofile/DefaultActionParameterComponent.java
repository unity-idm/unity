/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

public class DefaultActionParameterComponent extends TextField implements ActionParameterComponent
{
	protected Binder<StringValueBean> binder;
	private String label;
	
	public DefaultActionParameterComponent(ActionParameterDefinition desc, MessageSource msg)
	{
		setLabel(desc.getName() + ":");
		setTooltipText(msg.getMessage(desc.getDescriptionKey()));
		binder = new Binder<>(StringValueBean.class);
		configureBinding(msg, desc.isMandatory());
		setWidth(TEXT_FIELD_MEDIUM.value());

	}
	
	protected void configureBinding(MessageSource msg, boolean mandatory)
	{	
		if (mandatory)
		{
			binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
			.bind("value");
		}
		else
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

	@Override
	public void setReadOnly(boolean readOnly)
	{
		super.setReadOnly(readOnly);
	}
	
	@Override
	public void setLabel(String label)
	{
		this.label = label;
		super.setLabel(label);
	}

	@Override
	public String getLabel()
	{
		return label;
	}
}
