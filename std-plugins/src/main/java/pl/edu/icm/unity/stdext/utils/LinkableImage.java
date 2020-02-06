/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.stdext.utils;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;

public class LinkableImage
{
	public static final LinkableImage EMPTY = new LinkableImage(null, null);
	
	private static final String JSON_IMAGE_PROPERTY_NAME = "image";
	private static final String JSON_URL_PROPERTY_NAME = "url";
	
	private final UnityImage image;
	private final URL url;

	public LinkableImage(UnityImage image, URL url)
	{
		this.image = image;
		this.url = url;
	}

	public UnityImage getUnityImage()
	{
		return image;
	}

	public URL getUrl()
	{
		return url;
	}
	
	public String toJsonString()
	{
		ObjectNode node = Constants.MAPPER.createObjectNode();
		node.put(JSON_IMAGE_PROPERTY_NAME, image == null ? null : image.serialize());
		node.put(JSON_URL_PROPERTY_NAME, url == null ? null : url.toExternalForm());
		return node.toString();
	}

	public static LinkableImage valueOf(String stringRepresentation) throws IOException
	{
		ObjectNode node = (ObjectNode) Constants.MAPPER.readTree(stringRepresentation);
		
		String serializedImage = JsonUtil.getNullable(node, JSON_IMAGE_PROPERTY_NAME);
		UnityImage image = null;
		if (serializedImage != null)
		{
			image = new UnityImage(serializedImage);
		}
		
		String serializedURL = JsonUtil.getNullable(node, JSON_URL_PROPERTY_NAME);
		URL url = null;
		if (serializedURL != null)
		{
			url = new URL(serializedURL);
			String protocol = url.getProtocol();
			if (!"http".equals(protocol) && !"https".equals(protocol))
			{
				throw new IllegalArgumentException("Only the http and https protocols are "
						+ "supported, provided: " + protocol);
			}
		}
		
		return new LinkableImage(image, url);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(image, url);
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof LinkableImage)
		{
			LinkableImage that = (LinkableImage) object;
			return Objects.equals(this.image, that.image) && Objects.equals(this.url, that.url);
		}
		return false;
	}
}
