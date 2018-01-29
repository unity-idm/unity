/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.boundededitors;

import com.vaadin.data.Binder;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * @author K. Benedyczak
 */
public class AbstractBoundEditor<T extends Number> extends CustomField<ValueWrapper>
{
	protected UnityMessageSource msg;
	protected T min;
	protected T max;
	protected T bound;

	private CheckBox unlimited;
	private TextField limit;
	private Binder<ValueWrapper> binder;
	
	public AbstractBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit,
			T bound, T min, T max)
	{
		this.msg = msg;
		this.bound = bound;
		this.min = min;
		this.max = max;
		
		setCaption(labelLimit);
		unlimited = new CheckBox();
		unlimited.setCaption(labelUnlimited);
		limit = new TextField();
		binder = new Binder<>(ValueWrapper.class);
		binder.bind(limit, "value");
		binder.bind(unlimited, "unlimited");
		binder.setBean(new ValueWrapper("", false));
		limit.setLocale(msg.getLocale());
		unlimited.addValueChangeListener(event ->
		{
			boolean limited = !unlimited.getValue();
			limit.setEnabled(limited);
			fireEvent(event);
		});
		limit.addValueChangeListener(event ->
		{
			fireEvent(event);
		});
		
	}

	@Override
	public ValueWrapper getValue()
	{
		return new ValueWrapper(limit.getValue(), unlimited.getValue());
	}

	@Override
	protected void doSetValue(ValueWrapper value)
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

	@Override
	protected Component initContent()
	{
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponents(limit, unlimited);
		hl.setComponentAlignment(limit, Alignment.MIDDLE_LEFT);
		hl.setComponentAlignment(unlimited, Alignment.MIDDLE_LEFT);
		hl.addStyleName(Styles.smallSpacing.toString());
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
}
