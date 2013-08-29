/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.boundededitors;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.UserError;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * <p>
 * Implementation note: Vaadin's {@link CustomField} overriding is far from trivial when advanced topics come into
 * play (validation, converters of individual fields). After long evaluation it seems that the simplest way
 * is to perform the whole logic in value change listeners.
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
	protected Converter<String, ?> converter;
	
	public AbstractBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit, final T bound, 
			Converter<String, ?> converter)
	{
		this.bound = bound;
		this.msg = msg;
		this.min = null;
		this.max = null;
		this.converter = converter;
		
		setCaption(labelLimit);
		unlimited = new CheckBox();
		unlimited.setCaption(labelUnlimited);
		limit = new TextField();
		limit.setConverter(converter);
		limit.setLocale(msg.getLocale());
		limit.setNullRepresentation("");
		limit.setValidationVisible(false);
		setValidationVisible(true);
		unlimited.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
			{
				boolean limited = !unlimited.getValue();
				limit.setEnabled(limited);
				setComponentError(null);
				if (limited)
					internalSetValueFromLimit();
				else
					setValue(bound);
			}
		});
		limit.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
			{
				boolean limited = !unlimited.getValue();
				setComponentError(null);
				if (limited)
					internalSetValueFromLimit();
			}
		});
		addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
			{
				T value = getValue();
				if (bound.equals(value))
					unlimited.setValue(true);
				else
				{
					unlimited.setValue(false);
					limit.setConvertedValue(value);
				}
			}
		});
		updateValidators();
	}
	
	
	@SuppressWarnings("unchecked")
	private void internalSetValueFromLimit()
	{
		try
		{
			setValue((T) limit.getConvertedValue());
		} catch(Exception e)
		{
			UserError error = new UserError(e.getMessage());
			setComponentError(error);
		}
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
	
	public AbstractBoundEditor<T> setReadOnly()
	{
		unlimited.setReadOnly(true);
		limit.setReadOnly(true);
		return this;
	}

	protected abstract void updateValidators();
}
