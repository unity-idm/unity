/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create floating point number attributes easily.
 * @author K. Benedyczak
 */
public class FloatingPointAttribute extends Attribute
{
	public FloatingPointAttribute(String name, String groupPath,
			List<String> values, String remoteIdp, String translationProfile)
	{
		super(name, FloatingPointAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public FloatingPointAttribute(String name, String groupPath,
			List<String> values)
	{
		super(name, FloatingPointAttributeSyntax.ID, groupPath, values);
	}
}
