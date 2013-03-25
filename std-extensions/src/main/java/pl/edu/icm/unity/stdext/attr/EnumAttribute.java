/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.unicore.util.configuration.ConfigurationException;

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

	public <T extends Enum<T>> List<T> getEnumValues(Class<T> type) throws ConfigurationException
	{
		List<String> vals = getValues();
		if (vals == null)
			return null;
		List<T> ret = new ArrayList<T>();
		Map<String, T> enumVals = new HashMap<String, T>();
		T[] constants = type.getEnumConstants();
		for (T label: constants) 
			enumVals.put(label.name(), label);
		
		for (String val: vals)
			ret.add(enumVals.get(val));
		return ret;
	}
}
