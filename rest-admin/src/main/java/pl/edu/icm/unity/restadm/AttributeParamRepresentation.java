/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * {@link Attribute} wrapper, preparing it for JSON serialization. Used when attribute must be set.
 * @author K. Benedyczak
 */
public class AttributeParamRepresentation
{
	private List<Object> values;
	private String name;
	private String groupPath;
	private AttributeVisibility visibility = AttributeVisibility.full;
	
	@SuppressWarnings("unchecked")
	public AttributeParamRepresentation(Attribute<?> orig)
	{
		@SuppressWarnings("rawtypes")
		AttributeValueSyntax syntax = orig.getAttributeSyntax();
		values = new ArrayList<Object>(orig.getValues().size());
		for (Object value: orig.getValues())
		{
			values.add(syntax.serializeSimple(value));
		}
		
		this.name = orig.getName();
		this.groupPath = orig.getGroupPath();
		this.visibility = orig.getVisibility();
	}
	
	public AttributeParamRepresentation()
	{
	}
	
	public <T> Attribute<T> toAPIAttribute(AttributeValueSyntax<T> syntax)
	{
		List<T> deserializedValues = new ArrayList<T>(values.size());
		for (Object jsonValue: values)
			deserializedValues.add(syntax.deserializeSimple(jsonValue));
		return new Attribute<T>(name, syntax, groupPath, visibility, deserializedValues); 
	}
	
	public List<Object> getValues()
	{
		return values;
	}

	public String getName()
	{
		return name;
	}

	public String getGroupPath()
	{
		return groupPath;
	}

	public AttributeVisibility getVisibility()
	{
		return visibility;
	}

	public void setValues(List<Object> values)
	{
		this.values = values;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setGroupPath(String groupPath)
	{
		this.groupPath = groupPath;
	}

	public void setVisibility(AttributeVisibility visibility)
	{
		this.visibility = visibility;
	}
}