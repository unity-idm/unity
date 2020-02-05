/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.stdext.attr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.utils.ImageValidatorUtil;
import pl.edu.icm.unity.stdext.utils.LinkableImage;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.stdext.utils.UnityImageSpec.ImageType;

public class PublicLinkableImageSyntax extends BaseImageAttributeSyntax<LinkableImage>
{
	public static final String ID = "publicLinkableImage";

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(LinkableImage value) throws IllegalAttributeValueException
	{
		if (value.getUnityImage() != null)
			ImageValidatorUtil.validate(getConfig(), value.getUnityImage());
	}

	@Override
	public LinkableImage convertFromString(String stringRepresentation)
	{
		try
		{
			return LinkableImage.valueOf(stringRepresentation);
		} catch (MalformedURLException e)
		{
			throw new InternalException("Invalid URL addres.", e);

		} catch (IOException e)
		{
			throw new InternalException("Error encoding image from string.", e);
		} catch (Exception e)
		{
			throw new InternalException("Error encoding to linkable image.", e);
		}
	}

	@Override
	public String convertToString(LinkableImage value)
	{
		return value.toJsonString();
	}

	@Override
	public String serializeSimple(LinkableImage value)
	{
		if (value.getImage() != null)
			return "https://localhost:2443/wellKnownLinks/publicLinkableImage/484j3mndnjr9y4r";

		if (value.getUrl() != null)
			return value.getUrl().toExternalForm();

		return null;
	}

	@Override
	public LinkableImage deserializeSimple(String value) throws IllegalAttributeValueException
	{
		if (value == null)
			return LinkableImage.EMPTY;
		
		URL url = null;
		UnityImage image = null;
		try
		{
			url = new URL(value);
		} catch (MalformedURLException e1)
		{
			try
			{
				image = new UnityImage(value);
			} catch (IOException e2)
			{
				IllegalAttributeValueException ex = new IllegalAttributeValueException(
						value + " can not be deserialized to " + getValueSyntaxId(), e2);
				ex.addSuppressed(e1);
				throw ex;
			}
		}
		return new LinkableImage(image, url);
	}
	

	@Override
	public LinkableImage newImage(LinkableImage value, byte[] rawData, ImageType type)
	{
		URL url = value == null ? null : value.getUrl();
		return new LinkableImage(new UnityImage(rawData, type), url);
	}
	
	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<LinkableImage>
	{
		public Factory()
		{
			super(PublicLinkableImageSyntax.ID, PublicLinkableImageSyntax::new);
		}
	}
}
