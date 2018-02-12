/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;


/**
 * Jpeg image attribute value syntax. Allows for specifying size and dimension limits.
 * The input and output of the attribute is any BufferedImage, but internally the image is stored 
 * as the Jpeg image with all the consequences.
 * 
 * Note: for performance reasons the equals is implemented to always return false, so each image 
 * is always assumed to be different then another.
 * @author K. Benedyczak
 */
public class JpegImageAttributeSyntax implements AttributeValueSyntax<BufferedImage>
{
	public static final String ID = "jpegImage";
	private int maxWidth = Integer.MAX_VALUE;
	private int maxHeight = Integer.MAX_VALUE;
	private int maxSize = 1024*1024;
	
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

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(BufferedImage value) throws IllegalAttributeValueException
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
			value = convertType(value);
			ImageIO.write(value, "jpg", dos);
		} catch (IOException e)
		{
			throw new IllegalAttributeValueException("Image can not be encoded as JPEG", e);
		}
		if (dos.getSize() > maxSize)
			throw new IllegalAttributeValueException("Image size after JPEG compression" +
					" (" + dos.getSize()  
					+ ") is too big, must be not greater than " + maxSize);
	}

	/**
	 * OpenJDK doesn't allow to JPEG encode Buffered images of certain types. For those 
	 * types this methods rewrites the source image into BufferedImage.TYPE_INT_RGB which is supported.
	 * For other cases the original image is returned.
	 * @param src
	 * @return
	 */
	private BufferedImage convertType(BufferedImage src)
	{
		int srcType = src.getType();
		if (srcType != BufferedImage.TYPE_INT_ARGB 
				&& srcType != BufferedImage.TYPE_INT_ARGB_PRE
				&& srcType != BufferedImage.TYPE_4BYTE_ABGR 
				&& srcType != BufferedImage.TYPE_4BYTE_ABGR_PRE)
			return src;
		
		BufferedImage bi2 = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = bi2.getGraphics();
		g.drawImage(src, 0, 0, Color.WHITE, null);
		g.dispose();
		return bi2;
	}
	
	@Override
	public boolean areEqual(BufferedImage value, Object anotherO)
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
			size+=b.length;
		}
		
		@Override
		public void write(byte b[], int off, int len) throws IOException 
		{
			size+=len;
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
	
	/**
	 * it is assumed that we have a Base64 encoded JPEG
	 */
	@Override
	public BufferedImage convertFromString(String stringRepresentation)
	{
		byte[] binary = Base64.getDecoder().decode(stringRepresentation);
		return deserialize(binary);
	}

	@Override
	public String convertToString(BufferedImage value)
	{
		byte[] binary = serialize(value);
		return Base64.getEncoder().encodeToString(binary);
	}
	
	public byte[] serialize(BufferedImage value) throws InternalException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
		try
		{
			value = convertType(value);
			ImageIO.write(value, "jpg", bos);
		} catch (IOException e)
		{
			throw new InternalException("Image can not be encoded as JPEG", e);
		}
	        return bos.toByteArray(); 
	}

	public BufferedImage deserialize(byte[] raw) throws InternalException
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(raw);
		try
		{
			return ImageIO.read(bis);
		} catch (IOException e)
		{
			throw new InternalException("Image can not be decoded", e);
		}
	}
	
	
	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<BufferedImage>
	{
		public Factory()
		{
			super(JpegImageAttributeSyntax.ID, JpegImageAttributeSyntax::new);
		}
	}
}


