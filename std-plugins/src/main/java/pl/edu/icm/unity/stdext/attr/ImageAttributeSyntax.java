/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.utils.BufferedImageWithExt;

import java.util.Base64;


/**
 * Image attribute value syntax.
 *
 * @author R. Ledzinski
 */
public class ImageAttributeSyntax extends AbstractImageAttributeSyntax<BufferedImageWithExt>
{
	public static final String ID = "image";

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(BufferedImageWithExt value) throws IllegalAttributeValueException {
		super.validate(value.getImage(), value.getType().toExt());
	}

	@Override
	public BufferedImageWithExt convertFromString(String stringRepresentation)
	{
		if (stringRepresentation.length() < 3) {
			throw new InternalException("Image can not be encoded - imageRepresentation (" + stringRepresentation +
					") too short");
		}
		String typeStr = stringRepresentation.substring(0, 3);
		return new BufferedImageWithExt(convertFromStringToBI(stringRepresentation.substring(3)),
				BufferedImageWithExt.ImageType.valueOf(typeStr));
	}

	@Override
	public String convertToString(BufferedImageWithExt value)
	{
		byte[] binary = serialize(value.getImage(), value.getType());
		return value.getType().toString() + Base64.getEncoder().encodeToString(binary);
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<BufferedImageWithExt>
	{
		public Factory()
		{
			super(ImageAttributeSyntax.ID, ImageAttributeSyntax::new);
		}
	}
}


