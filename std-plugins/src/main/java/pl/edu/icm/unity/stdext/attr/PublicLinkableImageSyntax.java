/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.CONTEXT_PATH;
import static pl.edu.icm.unity.engine.api.wellknown.AttributesContentServletProvider.SERVLET_PATH;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import org.apache.http.entity.ContentType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pl.edu.icm.unity.attr.LinkableImage;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.SharedAttributeContent;
import pl.edu.icm.unity.engine.api.attributes.SharedAttributeInfo;
import pl.edu.icm.unity.engine.api.attributes.SharedAttributeSpec;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.utils.ImageValidatorUtil;

public class PublicLinkableImageSyntax extends BaseImageAttributeSyntax<LinkableImage>
{
	public static final String ID = "publicLinkableImage";
	private static final String PUBLIC_IMAGE_PATH = CONTEXT_PATH + SERVLET_PATH + "/";
	
	private final URL serverAdvertisedAddress;

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}
	
	public PublicLinkableImageSyntax(URL serverAdvertisedAddress) 
	{
		this.serverAdvertisedAddress = serverAdvertisedAddress;
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
		{
			return getImageUrl(value);
		}

		if (value.getUrl() != null)
			return value.getUrl().toExternalForm();

		return "";
	}
	
	public String getImageUrl(LinkableImage value)
	{
		return serverAdvertisedAddress.toExternalForm() + PUBLIC_IMAGE_PATH + value.getExternalId();
	}

	@Override
	public LinkableImage deserializeSimple(String value) throws IllegalAttributeValueException
	{
		if (StringUtils.isEmpty(value))
			return LinkableImage.EMPTY;
		
		try
		{
			URL url = new URL(value);
			UUID externalId = null;
			if (url.getPath().startsWith(PUBLIC_IMAGE_PATH))
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
	public Optional<SharedAttributeSpec> shareSpec()
	{
		return Optional.of(new PublicLinkableImageSharingSpec());
	}
	
	private class PublicLinkableImageSharingSpec implements SharedAttributeSpec
	{
		@Override
		public SharedAttributeInfo getInfo(String stringRepresentation)
		{
			LinkableImage value = convertFromString(stringRepresentation);
			String externalId = value.getExternalId() == null ? null : value.getExternalId().toString();
			return new SharedAttributeInfo(externalId);
		}

		@Override
		public SharedAttributeContentProvider getContentProvider()
		{
			return stringRepresentation -> 
			{
				LinkableImage value = convertFromString(stringRepresentation);
				UnityImage image = value.getUnityImage();
				return new SharedAttributeContent(image.getImage(), getContentType(image));
			};
		}
		
		private ContentType getContentType(UnityImage image)
		{
			return ContentType.getByMimeType(image.getType().getMimeType());
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
