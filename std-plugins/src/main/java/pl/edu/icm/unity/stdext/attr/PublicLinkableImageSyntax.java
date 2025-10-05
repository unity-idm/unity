/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.CONTEXT_PATH;
import static pl.edu.icm.unity.engine.api.wellknown.AttributesContentPublicServletProvider.SERVLET_PATH;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.attribute.image.LinkableImage;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.PublicAttributeContent;
import pl.edu.icm.unity.engine.api.attributes.PublicAttributeInfo;
import pl.edu.icm.unity.engine.api.attributes.PublicAttributeSpec;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.utils.URLFactory;
import pl.edu.icm.unity.stdext.utils.ImageValidatorUtil;

public class PublicLinkableImageSyntax extends BaseImageAttributeSyntax<LinkableImage>
{
	public static final String ID = "publicLinkableImage";
	private static final String PUBLIC_IMAGE_PATH = CONTEXT_PATH + SERVLET_PATH + "/";
	
	private final URL serverAdvertisedAddress;

	public PublicLinkableImageSyntax(URL serverAdvertisedAddress) 
	{
		this.serverAdvertisedAddress = serverAdvertisedAddress;
	}

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
			return getImageUrl(value);

		if (value.getUrl() != null)
			return value.getUrl().toExternalForm();

		return "";
	}
	
	public String getImageUrl(LinkableImage value)
	{
		return getServletUrl() + value.getExternalId();
	}
	
	private String getServletUrl()
	{
		return serverAdvertisedAddress.toExternalForm() + PUBLIC_IMAGE_PATH;
	}

	@Override
	public LinkableImage deserializeSimple(String value) throws IllegalAttributeValueException
	{
		if (!StringUtils.hasLength(value))
			return LinkableImage.EMPTY;
		
		try
		{
			URL url = URLFactory.of(value);
			UUID externalId = null;
			if (value.startsWith(getServletUrl()))
			{
				String externalIdString = url.getPath().replace(PUBLIC_IMAGE_PATH, "");
				externalId = UUID.fromString(externalIdString);
			}
			return new LinkableImage(url, externalId);
		} catch (MalformedURLException | IllegalArgumentException e)
		{
			throw new IllegalAttributeValueException(value + " can not be deserialized to " + getValueSyntaxId(), e);
		}
	}
	
	@Override
	public Optional<PublicAttributeSpec> publicExposureSpec()
	{
		return Optional.of(new PublicLinkableImageSharingSpec());
	}
	
	private class PublicLinkableImageSharingSpec implements PublicAttributeSpec
	{
		@Override
		public PublicAttributeInfo getInfo(String stringRepresentation)
		{
			LinkableImage value = convertFromString(stringRepresentation);
			String externalId = value.getExternalId() == null ? null : value.getExternalId().toString();
			return new PublicAttributeInfo(externalId);
		}

		@Override
		public PublicAttributeContentProvider getContentProvider()
		{
			return stringRepresentation -> 
			{
				LinkableImage value = convertFromString(stringRepresentation);
				UnityImage image = value.getUnityImage();
				return new PublicAttributeContent(image.getImage(), image.getType().getMimeType());
			};
		}
	}
	
	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<LinkableImage>
	{
		public Factory(AdvertisedAddressProvider advertisedAddressProvider)
		{
			super(PublicLinkableImageSyntax.ID, () -> new PublicLinkableImageSyntax(advertisedAddressProvider.get()));
		}
	}
}
