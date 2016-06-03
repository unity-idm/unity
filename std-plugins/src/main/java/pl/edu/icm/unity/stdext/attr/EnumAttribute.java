/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.List;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Helper class allowing to create enumeration attributes easily.
 * @author K. Benedyczak
 */
public class EnumAttribute extends Attribute
{
	public EnumAttribute(String name, String groupPath, List<String> values,
			String remoteIdp, String translationProfile)
	{
		super(name, EnumAttributeSyntax.ID, groupPath, values, remoteIdp, translationProfile);
	}

	public EnumAttribute(String name, String groupPath, List<String> values)
	{
		super(name, EnumAttributeSyntax.ID, groupPath, values);
	}
}
