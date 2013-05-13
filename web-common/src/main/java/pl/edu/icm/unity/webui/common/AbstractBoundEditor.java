/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * @author K. Benedyczak
 */
public abstract class AbstractBoundEditor<T extends Number>
{
	protected UnityMessageSource msg;
	protected CheckBox unlimited;
	protected TextField limit;
	protected T bound;
	protected T min;
	protected T max;
	
	public AbstractBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit, T bound, 
			T min, T max, Converter<String, ?> converter)
	{
		this.bound = bound;
		this.msg = msg;
		this.min = min;
		this.max = max;
		unlimited = new CheckBox();
		unlimited.setCaption(labelUnlimited);
		limit = new TextField();
		limit.setConverter(converter);
		limit.setRequired(true);
		limit.setRequiredError(msg.getMessage("fieldRequired"));
		limit.setCaption(labelLimit);
		unlimited.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				boolean limited = !unlimited.getValue();
				limit.setEnabled(limited);
			}
		});
		updateValidators();
	}
	
	public void addToLayout(FlexibleFormLayout layout)
	{
		layout.addLine(limit, unlimited);
	}
	
	public AbstractBoundEditor<T> setMin(T min)
	{
		this.min = min;
		updateValidators();
		return this;
	}

	
	
	public AbstractBoundEditor<T> setMax(T max)
	{
		this.max = max;
		updateValidators();
		return this;
	}
	
	public AbstractBoundEditor<T> setValue(T value)
	{
		if (!value.equals(bound))
		{
			limit.setValue(encodeValue(value));
			unlimited.setValue(false);
			limit.setEnabled(true);
		} else
		{
			unlimited.setValue(true);	
			limit.setEnabled(false);
		}
		return this;
	}
	
	public AbstractBoundEditor<T> setReadOnly()
	{
		unlimited.setReadOnly(true);
		limit.setReadOnly(true);
		return this;
	}
	
	public T getValue() throws IllegalStateException
	{
		if (unlimited.getValue())
			return bound;
		String val = limit.getValue();
		return parseValue(val);
	}
	
	protected abstract T parseValue(String value);
	protected abstract String encodeValue(T value);
	protected abstract void updateValidators();
}
