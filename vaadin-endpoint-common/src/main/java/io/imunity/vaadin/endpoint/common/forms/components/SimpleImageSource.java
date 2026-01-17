/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms.components;

import io.imunity.vaadin.endpoint.common.file.ImageUtils;
import pl.edu.icm.unity.base.exceptions.InternalException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class SimpleImageSource
{
	private static final String PNG_MIME_TYPE = "image/png";
	private final byte[] data;

	public SimpleImageSource(BufferedImage value)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
		try
		{
			ImageIO.write(value, "png", bos);
		}
		catch (IOException e)
		{
			throw new InternalException("Image can not be encoded as PNG", e);
		}
		data = bos.toByteArray();
	}

	public String getDataUrl()
	{
		return ImageUtils.createDataUrl(data, PNG_MIME_TYPE);
	}

	public byte[] getData()
	{
		return data;
	}
}
