/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pl.edu.icm.unity.attr.LinkableImage;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.utils.ImageValidatorUtil;

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
		if (value.getUnityImage() != null)
			return "https://localhost:2443/wellKnownLinks/publicLinkableImage/484j3mndnjr9y4r";

		if (value.getUrl() != null)
			return value.getUrl().toExternalForm();

		return "";
	}

	@Override
	public LinkableImage deserializeSimple(String value) throws IllegalAttributeValueException
	{
		if (StringUtils.isEmpty(value))
			return LinkableImage.EMPTY;
		
		try
		{
			URL url = new URL(value);
			return new LinkableImage(url);
		} catch (MalformedURLException e1)
		{
			throw new IllegalAttributeValueException(value + " can not be deserialized to " + getValueSyntaxId());
		}
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
