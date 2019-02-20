/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * Action parameter for boolean 
 * @author P.Piernik
 *
 */
public class BooleanActionParameterComponent extends CheckBox implements ActionParameterComponent
{	
	private Binder<StringValueBean> binder;
	
	public BooleanActionParameterComponent(ActionParameterDefinition desc,
			UnityMessageSource msg)
	{
		super(desc.getName());
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		binder = new Binder<>(StringValueBean.class);
		binder.forField(this).withConverter(v -> String.valueOf(v),
					v -> Boolean.valueOf(v)).bind("value");
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
}
