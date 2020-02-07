/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import java.net.URL;

import javax.annotation.Nullable;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.attr.LinkableImage;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;

class PublicLinkableImageValueComponent extends CustomComponent
{
	private final UnityImageValueComponent imageValueComponent;
	private final ExternalUrlOptionComponent urlValueComponent;
	private final RadioButtonGroup<Option> selector;
	
	private enum Option
	{
		EMBEDDED_IMAGE,
		EXTERNAL_IMAGE_URL
	}

	PublicLinkableImageValueComponent(@Nullable LinkableImage value,
			ImageConfiguration imgConfig,
			UnityMessageSource msg)
	{
		UnityImage unityImage = value == null ? null : value.getUnityImage();
		URL url = value == null ? null : value.getUrl();
		Option activeOption = unityImage != null ? Option.EMBEDDED_IMAGE : Option.EXTERNAL_IMAGE_URL;
		
		imageValueComponent = new UnityImageValueComponent(unityImage, imgConfig, msg);
		urlValueComponent = new ExternalUrlOptionComponent(url, msg);
		
		if (activeOption == Option.EMBEDDED_IMAGE)
			urlValueComponent.setVisible(false);
		else
			imageValueComponent.setVisible(false);
		
		selector = new RadioButtonGroup<>(null, DataProvider.ofItems(Option.values()));
		selector.setSelectedItem(activeOption);
		selector.setItemCaptionGenerator(item -> msg.getMessage("PublicLinkableImage.option." + item.name()));
		selector.addValueChangeListener(this::valueChange);
		
		VerticalLayout layout = new VerticalLayout();
		layout.addComponents(selector, imageValueComponent, urlValueComponent);
		layout.setMargin(false);
		layout.setSpacing(true);
		setCompositionRoot(layout);
	}

	LinkableImage getValue(boolean required, PublicLinkableImageSyntax syntax) throws IllegalAttributeValueException
	{
		Option selected = selector.getSelectedItem().orElse(null);
		if (selected == null)
			new IllegalAttributeValueException("BUG: Select and configure one of the options");
		
		if (selected == Option.EMBEDDED_IMAGE)
		{
			UnityImage image = imageValueComponent.getValue(required, new LinkableImageValidator(syntax));
			return new LinkableImage(image);
		}
		
		URL url = urlValueComponent.getValue(required);
		return new LinkableImage(url); 
	}
	
	private void valueChange(ValueChangeEvent<Option> event)
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
			syntax.validate(new LinkableImage(value));
		}
	}
}
