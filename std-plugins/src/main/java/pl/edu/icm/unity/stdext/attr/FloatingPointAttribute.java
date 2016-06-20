/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;
import java.util.stream.Collectors;

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

	public FloatingPointAttribute(String name, String groupPath, List<Double> values)
	{
		super(name, FloatingPointAttributeSyntax.ID, groupPath, convert(values));
	}
	
	private static List<String> convert(List<Double> values)
	{
		FloatingPointAttributeSyntax syntax = new FloatingPointAttributeSyntax();
		return values.stream().
				map(v -> syntax.convertToString(v)).
				collect(Collectors.toList());
	}
}
