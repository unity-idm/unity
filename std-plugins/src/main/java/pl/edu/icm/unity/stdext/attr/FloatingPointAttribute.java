/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;

/**
 * Helper class allowing to create floating point number attributes easily.
 * @author K. Benedyczak
 */
public class FloatingPointAttribute
{
	public static Attribute of(String name, String groupPath,
			List<String> values, String remoteIdp, String translationProfile)
	{
		return new Attribute(name, FloatingPointAttributeSyntax.ID, groupPath, 
				values, remoteIdp, translationProfile);
	}

	public static Attribute of(String name, String groupPath, List<Double> values)
	{
		return new Attribute(name, FloatingPointAttributeSyntax.ID, groupPath, convert(values));
	}
	
	public static Attribute of(String name, String groupPath, Double... values)
	{
		return of(name, groupPath, Lists.newArrayList(values));
	}

	private static List<String> convert(List<Double> values)
	{
		FloatingPointAttributeSyntax syntax = new FloatingPointAttributeSyntax();
		return values.stream().
				map(v -> syntax.convertToString(v)).
				collect(Collectors.toList());
	}
}
