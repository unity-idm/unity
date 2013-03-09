/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Helper class allowing to create enumeration attributes easily.
 * @author K. Benedyczak
 */
public class EnumAttribute extends Attribute<String>
{
	public EnumAttribute(String name, String groupPath, AttributeVisibility visibility,
			List<String> values)
	{
		super(name, new EnumAttributeSyntax(), groupPath, visibility, values);
	}

	public EnumAttribute(String name, String groupPath, AttributeVisibility visibility,
			String value)
	{
		this(name, groupPath, visibility, Collections.singletonList(value));
	}
	
	public EnumAttribute()
	{
	}

}
