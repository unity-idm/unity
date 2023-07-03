/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.attribute.image.LinkableImage;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

@Tag("div")
class PublicLinkableImageValueComponent extends Component implements HasComponents, HasLabel
{
	private final UnityImageValueComponent imageValueComponent;
	private final ExternalUrlOptionComponent urlValueComponent;
	private final RadioButtonGroup<Option> selector;
	private final UUID externalId;

	private enum Option
	{
		EMBEDDED_IMAGE,
		EXTERNAL_IMAGE_URL
	}
	
	PublicLinkableImageValueComponent(@Nullable LinkableImage value,
	                                  ImageConfiguration imgConfig,
	                                  MessageSource msg)
	{
		UnityImage unityImage = value == null ? null : value.getUnityImage();
		URL url = value == null ? null : value.getUrl();
		externalId = value == null ? UUID.randomUUID() : value.getExternalId();
		Option activeOption = unityImage != null || value == null ? Option.EMBEDDED_IMAGE : Option.EXTERNAL_IMAGE_URL;
		
		imageValueComponent = new UnityImageValueComponent(unityImage, imgConfig, msg);
		urlValueComponent = new ExternalUrlOptionComponent(url);
		urlValueComponent.setWidthFull();
		
		if (activeOption == Option.EMBEDDED_IMAGE)
			urlValueComponent.setVisible(false);
		else
			imageValueComponent.setVisible(false);
		
		selector = new RadioButtonGroup<>(null, Option.values());
		imageValueComponent.addChangeListener(() -> selector.setInvalid(false));
		selector.setValue(activeOption);
		selector.setItemLabelGenerator(item -> msg.getMessage("PublicLinkableImage.option." + item.name()));
		selector.addValueChangeListener(this::valueChange);
		add(selector, imageValueComponent, urlValueComponent);
	}

	Optional<LinkableImage> getValue(boolean required, PublicLinkableImageSyntax syntax) throws IllegalAttributeValueException
	{
		Option selected = selector.getOptionalValue().orElse(null);
		if (selected == null)
			throw new IllegalAttributeValueException("BUG: Select and configure one of the options");
		
		if (selected == Option.EMBEDDED_IMAGE)
		{
			try
			{
				Optional<UnityImage> image = imageValueComponent.getValue(required, new LinkableImageValidator(syntax));
				return image.map(imageV -> new LinkableImage(imageV, externalId));
			}
			catch (IllegalAttributeValueException e)
			{
				selector.setInvalid(true);
				throw e;
			}
		}
		
		return urlValueComponent.getValue(required).map(url -> new LinkableImage(url, externalId));
	}
	
	private void valueChange(HasValue.ValueChangeEvent<Option> event)
	{
		if (event.getValue() == Option.EMBEDDED_IMAGE)
		{
			imageValueComponent.setVisible(true);
			urlValueComponent.setVisible(false);
		} else
		{
			imageValueComponent.setVisible(false);
			urlValueComponent.setVisible(true);
		}
		selector.setInvalid(false);
	}

	public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible)
	{
		selector.setRequiredIndicatorVisible(requiredIndicatorVisible);
	}

	@Override
	public void setLabel(String label) {
		selector.setLabel(label);
	}

	@Override
	public String getLabel() {
		return selector.getLabel();
	}
	
	private class LinkableImageValidator implements ImageValidator
	{
		private final PublicLinkableImageSyntax syntax;
		
		LinkableImageValidator(PublicLinkableImageSyntax syntax)
		{
			this.syntax = syntax;
		}

		@Override
		public void validate(UnityImage value) throws IllegalAttributeValueException
		{
			syntax.validate(new LinkableImage(value, externalId));
		}
	}
}
