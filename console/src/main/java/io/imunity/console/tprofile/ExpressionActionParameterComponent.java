/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.data.binder.Binder;

import io.imunity.console.views.directory_setup.automation.mvel.MVELExpressionField;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

/**
 * For editing MVEL expressions. Decorates the {@link MVELExpressionField} with
 * drag'n'drop support.
 */
public class ExpressionActionParameterComponent extends MVELExpressionField implements ActionParameterComponent
{
	private final Binder<StringValueBean> binder;
	private String label;

	public ExpressionActionParameterComponent(ActionParameterDefinition param, MessageSource msg)
	{
		super(msg, param.getName() + ":", msg.getMessage(param.getDescriptionKey()),
				(MVELExpressionContext) param.getDetails()
						.get());
		binder = new Binder<>(StringValueBean.class);
		configureBinding(binder, "value", param.isMandatory());
		binder.setBean(new StringValueBean());	
		setWidthFull();
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
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}

	@Override
	public void addValueChangeCallback(Runnable callback)
	{
		binder.addValueChangeListener((e) -> callback.run());
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