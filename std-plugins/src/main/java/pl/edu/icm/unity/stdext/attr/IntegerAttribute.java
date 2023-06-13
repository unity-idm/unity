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
 * Helper class allowing to create integer attributes easily.
 * @author K. Benedyczak
 */
public class IntegerAttribute
{
	public static Attribute of(String name, String groupPath,
			List<String> values, String remoteIdp, String translationProfile)
	{
		return new Attribute(name, IntegerAttributeSyntax.ID, groupPath, values, 
				remoteIdp, translationProfile);
	}

	public static Attribute of(String name, String groupPath, List<? extends Number> values)
	{
		return new Attribute(name, IntegerAttributeSyntax.ID, groupPath, convert(values));
	}
	
	public static Attribute of(String name, String groupPath, Long... values)
	{
		return of(name, groupPath, Lists.newArrayList(values));
	}

	public static Attribute of(String name, String groupPath, Integer... values)
	{
		return of(name, groupPath, Lists.newArrayList(values));
	}
	
	private static List<String> convert(List<? extends Number> values)
	{
		IntegerAttributeSyntax syntax = new IntegerAttributeSyntax();
		return values.stream().
				map(v -> syntax.convertToString(v.longValue())).
				collect(Collectors.toList());
	}
}
