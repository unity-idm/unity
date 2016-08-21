/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create string attributes easily.
 * @author K. Benedyczak
 */
public class StringAttribute extends Attribute
{
	public StringAttribute(String name, String groupPath,
			List<String> values, String remoteIdp, String translationProfile)
	{
		super(name, StringAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public StringAttribute(String name, String groupPath, List<String> values)
	{
		super(name, StringAttributeSyntax.ID, groupPath, values);
	}
	
	public StringAttribute(String name, String groupPath, String... values)
	{
		super(name, StringAttributeSyntax.ID, groupPath, Lists.newArrayList(values));
	}
	
	public static Attribute of(String name, String groupPath,
			List<String> values, String remoteIdp, String translationProfile)
	{
		return new Attribute(name, StringAttributeSyntax.ID, groupPath, values, remoteIdp, 
				translationProfile);
	}

	public static Attribute of(String name, String groupPath, List<String> values)
	{
		return new Attribute(name, StringAttributeSyntax.ID, groupPath, values);
	}
	
	public static Attribute of(String name, String groupPath, String... values)
	{
		return of(name, groupPath, Lists.newArrayList(values));
	}
}
