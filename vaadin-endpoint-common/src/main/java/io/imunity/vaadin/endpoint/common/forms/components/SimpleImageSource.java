/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms.components;

import com.vaadin.flow.server.StreamResource;
import pl.edu.icm.unity.base.exceptions.InternalException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

class SimpleImageSource
{
	private static final Random random = new Random();
	private final byte[] data;

	public SimpleImageSource(BufferedImage value)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
		try
		{
			ImageIO.write(value, "png", bos);
		} catch (IOException e)
		{
			throw new InternalException("Image can not be encoded as PNG", e);
		}
		data = bos.toByteArray();
	}

	public StreamResource getResource()
	{
		return new StreamResource("imgattribute-"+random.nextLong()+".png", () -> new ByteArrayInputStream(data));
	}
}
