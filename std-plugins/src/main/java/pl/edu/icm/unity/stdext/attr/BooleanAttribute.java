/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.primitives.Booleans;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create boolean attributes easily.
 * @author K. Benedyczak
 */
public class BooleanAttribute
{
	public static Attribute of(String name, String groupPath,
			List<Boolean> values, String remoteIdp, String translationProfile)
	{
		return new Attribute(name, BooleanAttributeSyntax.ID, groupPath, toString(values), remoteIdp, 
				translationProfile);
	}

	public static Attribute of(String name, String groupPath, List<Boolean> values)
	{
		return new Attribute(name, BooleanAttributeSyntax.ID, groupPath, toString(values));
	}
	
	public static Attribute of(String name, String groupPath, Boolean... values)
	{
		return of(name, groupPath, Lists.newArrayList(values));
	}

	public static Attribute of(String name, String groupPath, boolean... values)
	{
		return of(name, groupPath, Booleans.asList(values));
	}
	
	private static List<String> toString(List<Boolean> values)
	{
		return values.stream().map(v -> String.valueOf(v)).collect(Collectors.toList());
	}
}
