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

import javax.annotation.Nullable;

import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;
import pl.edu.icm.unity.stdext.utils.LinkableImage;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.webui.common.Styles;

class PublicLinkableImageValueComponent extends CustomComponent
{
	private final UnityImageValueComponent imageValueComponent;
	private final Binder<URLValue> binder;

	PublicLinkableImageValueComponent(@Nullable LinkableImage value,
			ImageConfiguration imgConfig,
			UnityMessageSource msg)
	{
		UnityImage unityImage = value == null ? null : value.getUnityImage();
		URL url = value == null ? null : value.getUrl();
		
		imageValueComponent = new UnityImageValueComponent(unityImage, imgConfig, msg);

		TextField urlField = new TextField();
		urlField.setStyleName(Styles.bottomMargin.toString());
		urlField.setPlaceholder(msg.getMessage("PublicLinkableImage.imageUrl"));
		binder = new Binder<>();
		binder.forField(urlField)
			.withValidator(new URLValidator())
			.bind(URLValue::toString, URLValue::valueOf);
		binder.setBean(new URLValue(url));
		
		VerticalLayout layout = new VerticalLayout();
		layout.addComponents(imageValueComponent, urlField);
		layout.setMargin(false);
		layout.setSpacing(true);
		setCompositionRoot(layout);
	}

	LinkableImage getValue(boolean required, PublicLinkableImageSyntax syntax) throws IllegalAttributeValueException
	{
		BinderValidationStatus<URLValue> status = binder.validate();
		if (!status.isOk())
		{
			String msg = status.getValidationErrors().stream()
					.map(ValidationResult::getErrorMessage)
					.collect(joining());
			throw new IllegalAttributeValueException(msg);
		}
		URL url = binder.getBean().getValue();
		UnityImage image = imageValueComponent.getValue(required, new LinkableImageValidator(syntax));
		return new LinkableImage(image, url);
	}
	
	private static class LinkableImageValidator implements ImageValidator
	{
		private final PublicLinkableImageSyntax syntax;
		
		LinkableImageValidator(PublicLinkableImageSyntax syntax)
		{
			this.syntax = syntax;
		}

		@Override
		public void validate(UnityImage value) throws IllegalAttributeValueException
		{
			PublicLinkableImageSyntax.validateImage(syntax.getConfig(), value);
		}
		
	}
	
	private static class URLValue
	{
		private URL value;

		URLValue(URL value)
		{
			super();
			this.value = value;
		}

		URL getValue()
		{
			return value;
		}

		void valueOf(String value)
		{
			if (value == null)
				return;
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
			if (value != null && !value.isEmpty())
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
