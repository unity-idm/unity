/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import pl.edu.icm.unity.base.message.MessageSource;

public class AbstractBoundEditor<T extends Number> extends CustomField<ValueWrapper>
{
	protected MessageSource msg;
	protected T min;
	protected T max;
	protected T bound;

	private final Checkbox unlimited;
	private final TextField limit;

	public AbstractBoundEditor(MessageSource msg, String labelUnlimited, Optional<String> labelLimit,
			T bound, T min, T max)
	{
		this.msg = msg;
		this.bound = bound;
		this.min = min;
		this.max = max;
		if (labelLimit.isPresent())
		{
			setLabel(labelLimit.get());
		}
		unlimited = new Checkbox();
		unlimited.setLabel(labelUnlimited);
		limit = new TextField();
		Binder<ValueWrapper> binder = new Binder<>(ValueWrapper.class);
		binder.bind(limit, "value");
		binder.bind(unlimited, "unlimited");
		binder.setBean(new ValueWrapper("", false));
		unlimited.addValueChangeListener(event ->
		{
			boolean limited = !unlimited.getValue();
			limit.setEnabled(limited);
		});
		limit.addValueChangeListener(this::fireEvent);
		add(initContent());
	}

	protected Component initContent()
	{
		HorizontalLayout hl = new HorizontalLayout();
		hl.add(limit, unlimited);
		hl.setAlignItems(FlexComponent.Alignment.CENTER);
		hl.setSpacing(true);
		return hl;
	}

	public TextField getLimitComponent()
	{
		return limit;
	}
	
	public AbstractBoundEditor<T> setMin(T min)
	{
		this.min = min;
		return this;
	}
	
	public AbstractBoundEditor<T> setMax(T max)
	{
		this.max = max;
		return this;
	}
	
	public AbstractBoundEditor<T> setReadOnly()
	{
		unlimited.setReadOnly(true);
		limit.setReadOnly(true);
		return this;
	}

	@Override
	protected ValueWrapper generateModelValue()
	{
		return new ValueWrapper(limit.getValue(), unlimited.getValue());
	}

	@Override
	protected void setPresentationValue(ValueWrapper value)
	{
		if (value.isUnlimited())
		{
			unlimited.setValue(true);
			limit.setValue("");
		} else
		{
			unlimited.setValue(false);
			limit.setValue(value.getValue());
		}
	}
}
