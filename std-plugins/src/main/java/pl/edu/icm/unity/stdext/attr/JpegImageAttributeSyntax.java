/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.Base64;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.attr.ImageType;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.utils.ImageValidatorUtil;

/**
 * Deprecated. Use ImageAttributeSyntax instead.
 *
 * @author K. Benedyczak
 */
public class JpegImageAttributeSyntax extends BaseImageAttributeSyntax<UnityImage>
{
	public static final String ID = "jpegImage";

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(UnityImage value) throws IllegalAttributeValueException
	{
		if (value.getType() != ImageType.JPG)
			throw new IllegalAttributeValueException("Invalid file format: " + value.getType().toExt());
		ImageValidatorUtil.validate(getConfig(), value);
	}

	/**
	 * it is assumed that we have a Base64 encoded JPEG
	 */
	@Override
	public UnityImage convertFromString(String stringRepresentation)
	{
		byte[] rawData = Base64.getDecoder().decode(stringRepresentation);
		return new UnityImage(rawData, ImageType.JPG);
	}

	@Override
	public String convertToString(UnityImage value)
	{
		return Base64.getEncoder().encodeToString(value.getImage());
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<UnityImage>
	{
		public Factory()
		{
			super(JpegImageAttributeSyntax.ID, JpegImageAttributeSyntax::new);
		}
	}
}
