/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;

/**
 * Helper class allowing to create enumeration attributes easily.
 * @author K. Benedyczak
 */
public class EnumAttribute
{
	public static Attribute of(String name, String groupPath, List<String> values,
			String remoteIdp, String translationProfile)
	{
		return new Attribute(name, EnumAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public static Attribute of(String name, String groupPath, List<String> values)
	{
		return new Attribute(name, EnumAttributeSyntax.ID, groupPath, values);
	}
	
	public static Attribute of(String name, String groupPath, String value)
	{
		return new Attribute(name, EnumAttributeSyntax.ID, groupPath, Lists.newArrayList(value));
	}
}
