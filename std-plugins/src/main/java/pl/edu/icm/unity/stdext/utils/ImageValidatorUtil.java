/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import pl.edu.icm.unity.base.attr.UnityImage;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;

public final class ImageValidatorUtil
{
	public static void validate(ImageConfiguration config, UnityImage value)
			throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		if (value.getWidth() > config.getMaxWidth())
			throw new IllegalAttributeValueException("Image width (" + value.getWidth()
					+ ") is too big, must be not greater than " + config.getMaxWidth());
		if (value.getHeight() > config.getMaxHeight())
			throw new IllegalAttributeValueException("Image height (" + value.getHeight()
					+ ") is too big, must be not greater than " + config.getMaxHeight());
		DiscardOutputStream dos = new DiscardOutputStream();
		String ext = value.getType().toExt();
		try
		{
			BufferedImage bufferedImage = value.getBufferedImage();
			ImageIO.write(bufferedImage, ext, dos);
		} catch (IOException e)
		{
			throw new IllegalAttributeValueException("Image can not be encoded as " + ext, e);
		}
		if (dos.getSize() > config.getMaxSize())
			throw new IllegalAttributeValueException("Image size after " + ext +
					" compression (" + dos.getSize()
					+ ") is too big, must be not greater than " + config.getMaxSize());
	}
	
	private static class DiscardOutputStream extends OutputStream
	{
		private int size = 0;

		@Override
		public void write(int b) throws IOException
		{
			size++;
		}

		@Override
		public void write(byte b[]) throws IOException
		{
			size += b.length;
		}

		@Override
		public void write(byte b[], int off, int len) throws IOException
		{
			size += len;
		}

		public int getSize()
		{
			return size;
		}
	}
	
	private ImageValidatorUtil() {}
}
