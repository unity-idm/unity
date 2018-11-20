/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

import java.awt.image.BufferedImage;
import java.util.Base64;


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
}


