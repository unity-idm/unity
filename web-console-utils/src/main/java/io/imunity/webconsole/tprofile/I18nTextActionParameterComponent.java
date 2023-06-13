/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.tprofile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.data.Binder;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

public class I18nTextActionParameterComponent extends I18nTextField implements ActionParameterComponent
{
	private Binder<StringValueBean> binder;
	
	public I18nTextActionParameterComponent(ActionParameterDefinition desc, MessageSource msg)
	{
		super(msg, desc.getName() + ":");
		setDescription(msg.getMessage(desc.getDescriptionKey()));	
		binder = new Binder<>(StringValueBean.class);
		
		if (desc.isMandatory())
		{
			binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
					.withConverter(v -> getString(v), v -> getI18nValue(v))
					.bind("value");
		} else
		{

			binder.forField(this).withConverter(v -> getString(v), v -> getI18nValue(v))
					.bind("value");
		}
		binder.setBean(new StringValueBean());
	}
	
	@Override
	public String getActionValue()
	{
		return binder.getBean().getValue();
	}

	@Override
	public void setActionValue(String value)
	{
		binder.setBean(new StringValueBean(value));
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
		binder.addValueChangeListener((e) -> { callback.run(); });	
	}

	@Override
	public boolean isValid()
	{
		binder.validate();
		return binder.isValid();
	}
	
	@Override
	public String getActionValueRepresentation(MessageSource msg)
	{
		 I18nString i18nValue = getI18nValue(getActionValue());
		 if (i18nValue != null)
			 return i18nValue.getDefaultLocaleValue(msg);
		 return null;
	}	

}

