/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.attribute.image.UnityImage;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.stdext.utils.ImageValidatorUtil;


/**
 * Image attribute value syntax.
 *
 * @author R. Ledzinski
 */
public class ImageAttributeSyntax extends BaseImageAttributeSyntax<UnityImage>
{
	public static final String ID = "image";

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(UnityImage value) throws IllegalAttributeValueException
	{
		ImageValidatorUtil.validate(getConfig(), value);
	}

	@Override
	public UnityImage convertFromString(String stringRepresentation)
	{
		try
		{
			return new UnityImage(stringRepresentation);
		}
		catch (IOException e) {
			throw new InternalException("Error encoding image from string.", e);
		}
	}

	@Override
	public String convertToString(UnityImage value)
	{
		return value.serialize();
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<UnityImage>
	{
		public Factory()
		{
			super(ImageAttributeSyntax.ID, ImageAttributeSyntax::new);
		}
	}
}


