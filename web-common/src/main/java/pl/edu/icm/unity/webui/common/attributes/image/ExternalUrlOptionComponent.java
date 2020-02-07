/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webui.common.attributes.image;

import static java.util.stream.Collectors.joining;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.webui.common.Styles;

class ExternalUrlOptionComponent extends CustomComponent
{
	private final Binder<URLValue> binder;
	
	ExternalUrlOptionComponent(URL initialValue, UnityMessageSource msg)
	{
		TextField urlField = new TextField();
		urlField.setStyleName(Styles.bottomMargin.toString());
		urlField.setRequiredIndicatorVisible(true);
		binder = new Binder<>();
		binder.forField(urlField)
			.withValidator(new URLValidator())
			.asRequired()
			.bind(URLValue::toString, URLValue::valueOf);
		binder.setBean(new URLValue(initialValue));
		setCompositionRoot(urlField);
	}
	
	URL getValue(boolean required) throws IllegalAttributeValueException
	{
		URL value = binder.getBean().getValue();
		if (value == null && !required)
			return null;

		BinderValidationStatus<URLValue> status = binder.validate();
		if (!status.isOk())
		{
			String msg = status.getValidationErrors().stream()
					.map(ValidationResult::getErrorMessage)
					.collect(joining());
			throw new IllegalAttributeValueException(msg);
		}
		
		return value;
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
