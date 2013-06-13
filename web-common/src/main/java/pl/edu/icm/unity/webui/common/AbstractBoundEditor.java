/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.data.util.converter.Converter;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * @author K. Benedyczak
 */
public abstract class AbstractBoundEditor<T extends Number> extends CustomField<T>
{
	protected UnityMessageSource msg;
	protected CheckBox unlimited;
	protected TextField limit;
	protected T bound;
	protected T min;
	protected T max;
	
	public AbstractBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit, T bound, 
			Converter<String, ?> converter)
	{
		this.bound = bound;
		this.msg = msg;
		this.min = null;
		this.max = null;
		
		setRequired(true);
		setCaption(labelLimit);

		unlimited = new CheckBox();
		unlimited.setCaption(labelUnlimited);
		limit = new TextField();
		limit.setConverter(converter);
		limit.setLocale(msg.getLocale());
		limit.setRequiredError(msg.getMessage("fieldRequired"));
		limit.setNullRepresentation("");
		unlimited.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
			{
				boolean limited = !unlimited.getValue();
				limit.setEnabled(limited);				
			}
		});
		updateValidators();
	}
	
	@Override
	protected Component initContent()
	{
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponents(limit, unlimited);
		return hl;
	}

	
	public TextField getLimitComponent()
	{
		return limit;
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
	
	public AbstractBoundEditor<T> setValueC(T value)
	{
		if (!value.equals(bound))
		{
			limit.setConvertedValue(value);
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
	
	@SuppressWarnings("unchecked")
	public T getValue() throws IllegalStateException
	{
		if (unlimited.getValue())
			return bound;
		return (T)limit.getConvertedValue();
	}
	
	protected abstract void updateValidators();
}
