/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms.components;

import io.imunity.vaadin.endpoint.common.file.DownloadHandlers;
import pl.edu.icm.unity.base.exceptions.InternalException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import com.vaadin.flow.server.streams.DownloadHandler;

class SimpleImageSource
{
	private static final Random random = new Random();
	private static final String PNG_MIME_TYPE = "image/png";
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

	public DownloadHandler getSrc()
	{
		return DownloadHandlers.forBytes(data, "%s.png".formatted(random.nextLong()), PNG_MIME_TYPE);
	}
}
