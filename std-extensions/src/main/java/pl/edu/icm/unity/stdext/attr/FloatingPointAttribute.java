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
 * Helper class allowing to create floating point number attributes easily.
 * @author K. Benedyczak
 */
public class FloatingPointAttribute extends Attribute<Double>
{
	public FloatingPointAttribute(String name, String groupPath, AttributeVisibility visibility,
			List<Double> values)
	{
		super(name, new FloatingPointAttributeSyntax(), groupPath, visibility, values);
	}

	public FloatingPointAttribute(String name, String groupPath, AttributeVisibility visibility,
			double value)
	{
		this(name, groupPath, visibility, Collections.singletonList(value));
	}

	public FloatingPointAttribute()
	{
	}
}
