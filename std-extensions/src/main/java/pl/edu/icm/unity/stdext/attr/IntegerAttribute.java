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
 * Helper class allowing to create integer attributes easily.
 * @author K. Benedyczak
 */
public class IntegerAttribute extends Attribute<Long>
{
	public IntegerAttribute(String name, String groupPath, AttributeVisibility visibility,
			List<Long> values)
	{
		super(name, new IntegerAttributeSyntax(), groupPath, visibility, values);
	}

	public IntegerAttribute(String name, String groupPath, AttributeVisibility visibility,
			long value)
	{
		this(name, groupPath, visibility, Collections.singletonList(value));
	}

	public IntegerAttribute()
	{
	}
}
