/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.utils.UnityImage;


/**
 * Deprecated. Use ImageAttributeSyntax instead.
 *
 * @author K. Benedyczak
 */
public class JpegImageAttributeSyntax extends AbstractImageAttributeSyntax<BufferedImage>
{
	public static final String ID = "jpegImage";

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(BufferedImage value) throws IllegalAttributeValueException {
		super.validate(value, "jpg");
	}

	/**
	 * it is assumed that we have a Base64 encoded JPEG
	 */
	 @Override
	public BufferedImage convertFromString(String stringRepresentation) {
		 return convertFromStringToBI(stringRepresentation);
	}

	@Override
	public String convertToString(BufferedImage value) {
		byte[] binary = serialize(value);
		return Base64.getEncoder().encodeToString(binary);
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<BufferedImage>
	{
		public Factory()
		{
			super(JpegImageAttributeSyntax.ID, JpegImageAttributeSyntax::new);
		}
	}

	/**
	 * Convert String into BufferedImage form
	 *
	 * @param stringRepresentation contains Base64 encoded image binary data (format depends on child class)
	 * @return Image
	 */
	public BufferedImage convertFromStringToBI(String stringRepresentation)
	{
		return deserialize(Base64.getDecoder().decode(stringRepresentation));
	}


	public byte[] serialize(BufferedImage value) throws InternalException
	{
		return serialize(value, UnityImage.ImageType.JPG);
	}

	/**
	 * Convert String into BufferedImage form
	 *
	 * @param value BufferedImage class
	 * @param type Original format of the image
	 * @return Image binary data in specified format
	 */
	public byte[] serialize(BufferedImage value,
							UnityImage.ImageType type) throws InternalException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
		try
		{
			value = UnityImage.convertType(value);
			ImageIO.write(value, type.toExt(), bos);
		} catch (IOException e)
		{
			throw new InternalException("Image can not be encoded as " + type, e);
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
}


