/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.ArrayList;
import java.util.List;

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
	
	/**
	 * When constructing from a full API attribute
	 * @param orig
	 */
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
	
	/**
	 * Mostly used by JSON deserialization
	 */
	public AttributeParamRepresentation()
	{
	}

	/**
	 * For manual construction of the object from scratch
	 */
	public AttributeParamRepresentation(String name, String group, List<?> values)
	{
		this.name = name;
		this.groupPath = group;
		this.values = new ArrayList<>(values.size());
		this.values.addAll(values);
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

	public void setValues(List<?> values)
	{
		this.values = new ArrayList<>(values.size());
		this.values.addAll(values);
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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupPath == null) ? 0 : groupPath.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeParamRepresentation other = (AttributeParamRepresentation) obj;
		if (groupPath == null)
		{
			if (other.groupPath != null)
				return false;
		} else if (!groupPath.equals(other.groupPath))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (values == null)
		{
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		if (visibility != other.visibility)
			return false;
		return true;
	}
}