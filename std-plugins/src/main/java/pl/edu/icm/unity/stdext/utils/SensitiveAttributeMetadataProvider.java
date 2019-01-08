/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.types.basic.AttributeType;

@Component
public class SensitiveAttributeMetadataProvider implements AttributeMetadataProvider 
{
	public static final String NAME = "securitySensitive";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Attribute is considered security sensitive and its changes by regular users will require "
				+ "additional authentication.";
	}

	@Override
	public void verify(String metadata, AttributeType at) throws IllegalAttributeTypeException
	{
	}

	@Override
	public boolean isSingleton()
	{
		return false;
	}

	@Override
	public boolean isSecuritySensitive()
	{
		return true;
	}
}
