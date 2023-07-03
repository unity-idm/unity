/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random;

import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;

import pl.edu.icm.unity.base.attribute.image.ImageType;
import pl.edu.icm.unity.base.attribute.image.UnityImage;

/**
 * Helper class providing image data as Resource
 *
 * @author R. Ledzinski
 */
public class SimpleImageSource implements StreamResource.StreamSource
{
	private static final Random random = new Random();
	
	private final byte[] isData;
	private final ImageType type;

	public SimpleImageSource(UnityImage image)
	{
		this.isData = image.getImage();
		this.type = image.getType();
	}

	@Override
	public InputStream getStream()
	{
		return new ByteArrayInputStream(isData);
	}

	public Resource getResource()
	{
		return new StreamResource(this, "imgattribute-" + random.nextLong() + random.nextLong()
				+ "." + type.toExt());
	}
}
