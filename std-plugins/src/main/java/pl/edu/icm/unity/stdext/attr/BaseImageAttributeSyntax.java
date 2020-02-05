/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.utils.ImageConfiguration;
import pl.edu.icm.unity.stdext.utils.UnityImageSpec.ImageType;

/**
 * Image attribute value syntax. Allows for specifying size and dimension
 * limits. The input and output of the attribute is any BufferedImage, but
 * internally the image is stored as original image format with all the
 * consequences.
 * <p>
 * Note: for performance reasons the equals is implemented to always return
 * false, so each image is always assumed to be different then another.
 * <p>
 * Common parent for:
 * 
 * @see JpegImageAttributeSyntax - kept to backward compatibility
 * @see ImageAttributeSyntax - new implementation of images supporing more
 *      formats
 *
 * @author K. Benedyczak, R. Ledzinski
 */
public abstract class BaseImageAttributeSyntax<T> implements AttributeValueSyntax<T>
{
	private final ImageConfiguration config = new ImageConfiguration();

	public abstract T newImage(T value, byte[] byteArray, ImageType type);
	
	@Override
	public JsonNode getSerializedConfiguration()
	{
		return config.getSerializedConfiguration();
	}

	@Override
	public void setSerializedConfiguration(JsonNode jsonN)
	{
		config.setSerializedConfiguration(jsonN);
	}

	public ImageConfiguration getConfig()
	{
		return config;
	}

	@Override
	public boolean areEqual(T value, Object anotherO)
	{
		return false;
	}
	
	@Override
	public int getMaxSize()
	{
		return config.getMaxSize();
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
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


