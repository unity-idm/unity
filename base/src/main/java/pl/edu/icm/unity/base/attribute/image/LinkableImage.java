/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.attribute.image;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.json.JsonUtil;

public class LinkableImage
{
	public static final LinkableImage EMPTY = new LinkableImage(null, null, null);
	
	private static final String JSON_IMAGE_PROPERTY_NAME = "image";
	private static final String JSON_URL_PROPERTY_NAME = "url";
	private static final String JSON_EXTERNAL_ID_PROPERTY_NAME = "externalId";
	
	private final UnityImage image;
	private final UUID externalId;
	private final URL url;
	
	public LinkableImage(UnityImage image, UUID externalId)
	{
		this(image, null, randomIfNull(externalId));
	}

	public LinkableImage(URL url, UUID externalId)
	{
		this(null, url, randomIfNull(externalId));
	}
	
	private LinkableImage(UnityImage image, URL url, UUID externalId)
	{
		this.image = image;
		this.url = url;
		this.externalId = externalId;
	}
	
	public UnityImage getUnityImage()
	{
		return image;
	}

	public URL getUrl()
	{
		return url;
	}
	
	public UUID getExternalId()
	{
		return externalId;
	}

	public String toJsonString()
	{
		ObjectNode node = Constants.MAPPER.createObjectNode();
		node.put(JSON_IMAGE_PROPERTY_NAME, image == null ? null : image.serialize());
		node.put(JSON_URL_PROPERTY_NAME, url == null ? null : url.toExternalForm());
		node.put(JSON_EXTERNAL_ID_PROPERTY_NAME, externalId == null ? null : externalId.toString());
		return node.toString();
	}

	public static LinkableImage valueOf(String stringRepresentation) throws IOException
	{
		ObjectNode node = (ObjectNode) Constants.MAPPER.readTree(stringRepresentation);
		
		String serializedImage = JsonUtil.getNullable(node, JSON_IMAGE_PROPERTY_NAME);
		UnityImage image = null;
		if (StringUtils.hasLength(serializedImage))
			image = new UnityImage(serializedImage);
		
		String serializedURL = JsonUtil.getNullable(node, JSON_URL_PROPERTY_NAME);
		URL url = null;
		if (StringUtils.hasLength(serializedURL))
		{
			url = new URL(serializedURL);
			String protocol = url.getProtocol();
			if (!"http".equals(protocol) && !"https".equals(protocol))
			{
				throw new IllegalArgumentException("Only the http and https protocols are "
						+ "supported, provided: " + protocol);
			}
		}
		
		String externalIdStr = JsonUtil.getNullable(node, JSON_EXTERNAL_ID_PROPERTY_NAME);
		UUID externalId = null;
		if (StringUtils.hasLength(externalIdStr))
		{
			externalId = UUID.fromString(externalIdStr);
		}
		
		return new LinkableImage(image, url, externalId);
	}

	private static UUID randomIfNull(UUID externalId)
	{
		return externalId == null ? UUID.randomUUID() : externalId;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(image, url, externalId);
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof LinkableImage)
		{
			LinkableImage that = (LinkableImage) object;
			return Objects.equals(this.image, that.image) && Objects.equals(this.url, that.url)
					&& Objects.equals(this.externalId, that.externalId);
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("LinkableImage [image=");
		builder.append(image);
		builder.append(", url=");
		builder.append(url);
		builder.append(", externalId=");
		builder.append(externalId);
		builder.append("]");
		return builder.toString();
	}
}
