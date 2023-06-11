/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.tprofile;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

public class IntegerActionParameterComponent extends IntStepper implements ActionParameterComponent
{
	private Binder<StringValueBean> binder;

	public IntegerActionParameterComponent(ActionParameterDefinition desc, MessageSource msg)
	{
		super(desc.getName());
		setDescription(msg.getMessage(desc.getDescriptionKey()));
		binder = new Binder<>(StringValueBean.class);
		binder.forField(this)
				.withConverter(v -> String.valueOf(v), v -> Integer.valueOf(v))
				.bind("value");
		binder.setBean(new StringValueBean(String.valueOf(0)));
	}

	@Override
	public String getActionValue()
	{
		return binder.getBean()
				.getValue();
	}

	@Override
	public void setActionValue(String value)
	{
		binder.setBean(new StringValueBean(value));
	}

	@Override
	public void addValueChangeCallback(Runnable callback)
	{
		binder.addValueChangeListener((e) ->
		{
			callback.run();
		});
	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}
}
