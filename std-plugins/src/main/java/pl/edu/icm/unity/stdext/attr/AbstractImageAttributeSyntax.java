/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.utils.UnityImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Image attribute value syntax. Allows for specifying size and dimension limits.
 * The input and output of the attribute is any BufferedImage, but internally the image is stored
 * as original image format with all the consequences.
 * <p>
 * Note: for performance reasons the equals is implemented to always return false, so each image
 * is always assumed to be different then another.
 * <p>
 * Common parent for:
 * - JpegImageAttributeSyntax - kept to backward compatibility
 * - ImageAttributeSyntax - new implementation of images supporing more formats
 *
 * @author K. Benedyczak, R. Ledzinski
 */
public abstract class AbstractImageAttributeSyntax<T> implements AttributeValueSyntax<T>
{
	int maxWidth = Integer.MAX_VALUE;
	int maxHeight = Integer.MAX_VALUE;
	int maxSize = 1024 * 1024;

	@Override
	public JsonNode getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("maxWidth", getMaxWidth());
		main.put("maxHeight", getMaxHeight());
		main.put("maxSize", getMaxSize());
		return main;
	}

	@Override
	public void setSerializedConfiguration(JsonNode jsonN)
	{
		maxWidth = jsonN.get("maxWidth").asInt();
		maxHeight = jsonN.get("maxHeight").asInt();
		maxSize = jsonN.get("maxSize").asInt();
	}

	public void validate(BufferedImage value, String ext) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");
		if (value.getWidth() > maxWidth)
			throw new IllegalAttributeValueException("Image width (" + value.getWidth()
					+ ") is too big, must be not greater than " + maxWidth);
		if (value.getHeight() > maxHeight)
			throw new IllegalAttributeValueException("Image height (" + value.getHeight()
					+ ") is too big, must be not greater than " + maxHeight);
		DiscardOutputStream dos = new DiscardOutputStream();
		try
		{
			BufferedImage bufferedImage = UnityImage.convertType(value);
			ImageIO.write(bufferedImage, ext, dos);
		} catch (IOException e)
		{
			throw new IllegalAttributeValueException("Image can not be encoded as " + ext, e);
		}
		if (dos.getSize() > maxSize)
			throw new IllegalAttributeValueException("Image size after " + ext +
					" compression (" + dos.getSize()
					+ ") is too big, must be not greater than " + maxSize);
	}

	@Override
	public boolean areEqual(T value, Object anotherO)
	{
		return false;
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	public int getMaxWidth()
	{
		return maxWidth;
	}

	public int getMaxHeight()
	{
		return maxHeight;
	}

	public void setMaxSize(int max) throws WrongArgumentException
	{
		if (max <= 0)
			throw new WrongArgumentException("Maximum size must be positive number");
		this.maxSize = max;
	}

	public int getMaxSize()
	{
		return maxSize;
	}

	public void setMaxWidth(int maxWidth) throws WrongArgumentException
	{
		if (maxWidth <= 0)
			throw new WrongArgumentException("Maximum width must be positive number");
		this.maxWidth = maxWidth;
	}

	public void setMaxHeight(int maxHeight) throws WrongArgumentException
	{
		if (maxHeight <= 0)
			throw new WrongArgumentException("Maximum height must be positive number");
		this.maxHeight = maxHeight;
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

	@Override
	public boolean isEmailVerifiable()
	{
		return false;
	}

	@Override
	public boolean isUserVerifiable()
	{
		return false;
	}
}


