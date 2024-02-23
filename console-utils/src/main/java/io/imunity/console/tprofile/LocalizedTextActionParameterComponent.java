/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

public class LocalizedTextActionParameterComponent extends LocalizedTextFieldDetails implements ActionParameterComponent
{
	private final Binder<StringValueBean> binder;
	private String caption;
	
	public LocalizedTextActionParameterComponent(ActionParameterDefinition desc, MessageSource msg)
	{
		super(msg.getEnabledLocales().values(), msg.getLocale());
		setLabel(desc.getName());
		setTooltipText(msg.getMessage(desc.getDescriptionKey()));
		setWidth(TEXT_FIELD_MEDIUM.value());
		binder = new Binder<>(StringValueBean.class);
		
		if (desc.isMandatory())
		{
			binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
					.withValidator(new TextActionValidator(msg))
					.withConverter(this::getString, this::getValue)
					.bind("value");
		} else
		{

			binder.forField(this).withConverter(this::getString, this::getValue)
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

	private Map<Locale, String> getValue(String value)
	{
		return getI18nValue(value) != null ? getI18nValue(value).getLocalizedMap() : new HashMap<>();
	}

	private I18nString getI18nValue(String value)
	{
		try
		{	if (value != null)
				return Constants.MAPPER.readValue(value, I18nString.class);
		else
			return null;
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Can't deserialize I18nString from JSON", e);
		}
	}
	
	private String getString(Map<Locale, String> value)
	{
		try
		{
			I18nString i18nString = new I18nString();
			i18nString.addAllValues(value.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getLanguage(), Map.Entry::getValue)));
			return Constants.MAPPER.writeValueAsString(i18nString.toJson());
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
	
	@Override
	public String getCaption()
	{
		return caption;
	}

	@Override
	public void setLabel(String label)
	{
		if(caption == null)
			caption = label;
		super.setLabel(label);
	}

	private static class TextActionValidator implements Validator<Map<Locale, String>>
	{
		private final MessageSource msg;

		public TextActionValidator(MessageSource msg)
		{
			this.msg = msg;
		}

		@Override
		public ValidationResult apply(Map<Locale, String> value, ValueContext context)
		{
			if (context.getHasValue().isPresent() && context.getHasValue().get().isEmpty())
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			if (value == null || value.values().stream().allMatch(String::isBlank))
				return ValidationResult.error(msg.getMessage("fieldRequired"));

			return ValidationResult.ok();
		}
	}
	
}

