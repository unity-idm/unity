/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

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
	private Binder<String> binder;
	private String value;
	
	public BooleanActionParameterComponent(ActionParameterDefinition desc,
			UnityMessageSource msg)
	{
		super(desc.getName());
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		binder = new Binder<>(String.class);
		binder.forField(this).bind(v -> Boolean.valueOf(v), (c, v) -> {
			value = String.valueOf(v);
		});
		value = String.valueOf(false);
		binder.setBean(value);
	}
	
	@Override
	public String getActionValue()
	{
		return this.value;
	}

	@Override
	public void setActionValue(String value)
	{
		this.value = value;
		binder.setBean(this.value);	
	}

	@Override
	public void addValueChangeCallback(Runnable callback)
	{
		addValueChangeListener((e) -> { callback.run(); });		
	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}
}
