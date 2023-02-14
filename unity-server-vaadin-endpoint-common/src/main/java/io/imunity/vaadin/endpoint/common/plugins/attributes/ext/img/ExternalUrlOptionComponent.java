/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.*;
import org.apache.commons.lang3.StringUtils;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

class ExternalUrlOptionComponent extends TextField
{
	private final Binder<URLValue> binder;
	
	ExternalUrlOptionComponent(URL initialValue)
	{
		setRequiredIndicatorVisible(true);
		binder = new Binder<>();
		binder.forField(this)
			.withValidator(new URLValidator())
			.asRequired()
			.bind(URLValue::toString, URLValue::valueOf);
		binder.setBean(new URLValue(initialValue));
	}
	
	Optional<URL> getValue(boolean required) throws IllegalAttributeValueException
	{
		URL value = binder.getBean().getValue();
		if (value == null && !required)
			return Optional.empty();

		BinderValidationStatus<URLValue> status = binder.validate();
		if (!status.isOk())
		{
			String msg = status.getValidationErrors().stream()
					.map(ValidationResult::getErrorMessage)
					.collect(joining());
			throw new IllegalAttributeValueException(msg);
		}
		
		return Optional.ofNullable(value);
	}
	
	private static class URLValue
	{
		private URL value;

		URLValue(URL value)
		{
			this.value = value;
		}

		URL getValue()
		{
			return value;
		}

		void valueOf(String value)
		{
			if (StringUtils.isEmpty(value))
			{
				this.value = null;
				return;
			}
			try
			{
				this.value = new URL(value);
			} catch (MalformedURLException e)
			{
				throw new IllegalStateException("BUG: value should be validated by binder", e);
			}
		}

		@Override
		public String toString()
		{
			return value == null ? null : value.toExternalForm();
		}
	}

	private static class URLValidator implements Validator<String>
	{
		@Override
		public ValidationResult apply(String value, ValueContext context)
		{
			if (!StringUtils.isEmpty(value))
			{
				try
				{
					new URL(value);
				} catch (MalformedURLException e)
				{
					return ValidationResult.error(e.getMessage());
				}
			}
			return ValidationResult.ok();
		}
	}
}
