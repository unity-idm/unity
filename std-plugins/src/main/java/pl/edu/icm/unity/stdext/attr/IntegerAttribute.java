/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create integer attributes easily.
 * @author K. Benedyczak
 */
public class IntegerAttribute extends Attribute
{
	public IntegerAttribute(String name, String groupPath,
			List<String> values, String remoteIdp, String translationProfile)
	{
		super(name, IntegerAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public IntegerAttribute(String name, String groupPath, List<? extends Number> values)
	{
		super(name, IntegerAttributeSyntax.ID, groupPath, convert(values));
	}
	
	public IntegerAttribute(String name, String groupPath, Long... values)
	{
		this(name, groupPath, Lists.newArrayList(values));
	}

	public IntegerAttribute(String name, String groupPath, Integer... values)
	{
		this(name, groupPath, Lists.newArrayList(values));
	}
	
	private static List<String> convert(List<? extends Number> values)
	{
		IntegerAttributeSyntax syntax = new IntegerAttributeSyntax();
		return values.stream().
				map(v -> syntax.convertToString(v.longValue())).
				collect(Collectors.toList());
	}
}
