/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.vaadin.data.Binder;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * Trivial, {@link TextField} based implementation of {@link ActionParameterComponent}. 
 * @author K. Benedyczak
 */
public class DefaultActionParameterComponent extends TextField implements ActionParameterComponent
{
	protected String value;
	protected Binder<String> binder;

	public DefaultActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		this(desc, msg, false);
	}
	
	public DefaultActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg, boolean required)
	{
		setCaption(desc.getName() + ":");
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		configureBinding(msg, required);
	}
	
	protected void configureBinding(UnityMessageSource msg, boolean required)
	{
		binder = new Binder<>(String.class);
		if (required)
			binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
			.bind(v -> this.value, (c, v) -> {
				this.value = v;
			});
		else 
			binder.forField(this).bind(v -> this.value, (c, v) -> {
				this.value = v;
			});
		binder.setBean(new String());	
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
