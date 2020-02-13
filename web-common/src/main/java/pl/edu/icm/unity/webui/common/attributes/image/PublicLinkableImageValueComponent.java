/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import java.util.UUID;

import javax.annotation.Nullable;

import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.attr.LinkableImage;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.PublicLinkableImageSyntax;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;

class PublicLinkableImageValueComponent extends CustomComponent
{
	private final UnityImageValueComponent imageValueComponent;
	private final UUID externalId;
	
	PublicLinkableImageValueComponent(@Nullable LinkableImage value,
			ImageConfiguration imgConfig,
			UnityMessageSource msg)
	{
		UnityImage unityImage = value == null ? null : value.getUnityImage();
		externalId = value == null ? UUID.randomUUID() : value.getExternalId();
		
		imageValueComponent = new UnityImageValueComponent(unityImage, imgConfig, msg);
		
		setCompositionRoot(imageValueComponent);
	}

	LinkableImage getValue(boolean required, PublicLinkableImageSyntax syntax) throws IllegalAttributeValueException
	{
		UnityImage image = imageValueComponent.getValue(required, new LinkableImageValidator(syntax));
		return new LinkableImage(image, externalId);
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
