/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.elements.VaadinElementReadOnlySetter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

/**
 * Action parameter for boolean
 */
public class BooleanActionParameterComponent extends Checkbox implements ActionParameterComponent
{	
	private final Binder<StringValueBean> binder;
	private String label;

	public BooleanActionParameterComponent(ActionParameterDefinition desc,
			MessageSource msg)
	{
		super(desc.getName());
		setTooltipText(msg.getMessage(desc.getDescriptionKey()));
		binder = new Binder<>(StringValueBean.class);
		binder.forField(this)
				.withConverter(String::valueOf, Boolean::valueOf).bind("value");
		binder.setBean(new StringValueBean(String.valueOf(false)));
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

	@Override
	public void setReadOnly(boolean readOnly)
	{
		VaadinElementReadOnlySetter.setReadOnly(getElement(), readOnly);
	}
}
