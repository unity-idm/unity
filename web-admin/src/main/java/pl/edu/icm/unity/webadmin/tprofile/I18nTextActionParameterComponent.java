/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.data.Binder;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField2;

public class I18nTextActionParameterComponent extends I18nTextField2 implements ActionParameterComponent
{
	private Binder<String> binder;
	private String value;
	
	public I18nTextActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		super(msg, desc.getName() + ":");
		setDescription(msg.getMessage(desc.getDescriptionKey()));	
		binder = new Binder<>(String.class);
		binder.forField(this).withConverter(v -> getString(v), v -> getI18nValue(v))
				.bind(v -> value, (c, v) -> {
					value = v;
				});
		binder.setBean(new String());
	}
	
	@Override
	public String getActionValue()
	{
		return value;
	}

	@Override
	public void setActionValue(String value)
	{
		this.value = value;
		binder.setBean(this.value);
	}

	private I18nString getI18nValue(String value)
	{
		try
		{	if (value != null)
				return Constants.MAPPER.readValue(value, I18nString.class);
			else
				return null;
		} catch (Exception e)
		{
			throw new IllegalStateException("Can't deserialize I18nString from JSON", e);
		}
	}
	
	private String getString(I18nString value)
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(value.toJson());
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Can't serialize I18nString to JSON", e);
		}
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
