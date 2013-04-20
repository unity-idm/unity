/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.types.InitializationValidator;

/**
 * Attribute type defines rules for handling attributes. Particular values
 * are subject to constraints defined in {@link AttributeValueSyntax} interface, 
 * with pluggable implementation. This class adds universal functionality:
 * descriptions, values cardinality limits and more.
 * <p>
 * The class is compared only using the name.
 */
public class AttributeType implements InitializationValidator
{
	/**
	 * The attribute type can not be changed using management API (it is created
	 * internally).
	 */
	public static final int TYPE_IMMUTABLE_FLAG = 0x01;
	
	/**
	 * The attribute type instances can not be created, updated or removed using management API
	 * (there are specialized methods to manipulate such attributes). This flag rather makes sense only
	 * in combination with TYPE_IMMUTABLE_FLAG.
	 */
	public static final int INSTANCES_IMMUTABLE_FLAG = 0x02;
	
	private String description = "";
	private String name;
	private AttributeValueSyntax<?> valueType;
	private int minElements = 0;
	private int maxElements = 1;
	private boolean uniqueValues = false;
	private boolean selfModificable = false;
	private AttributeVisibility visibility = AttributeVisibility.full;
	private int flags = 0;
	
	
	public AttributeType()
	{
	}
	
	public AttributeType(String name, AttributeValueSyntax<?> syntax)
	{
		this.name = name;
		this.valueType = syntax;
	}
	
	public AttributeType(String name, AttributeValueSyntax<?> syntax, String description)
	{
		this.name = name;
		this.valueType = syntax;
		this.description = description;
	}
	
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		if (description == null)
			throw new IllegalArgumentException("Argument can not be null");
		this.description = description;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		if (name == null)
			throw new IllegalArgumentException("Argument can not be null");
		this.name = name;
	}
	public AttributeValueSyntax<?> getValueType()
	{
		return valueType;
	}
	public void setValueType(AttributeValueSyntax<?> valueType)
	{
		if (valueType == null)
			throw new IllegalArgumentException("Argument can not be null");
		this.valueType = valueType;
	}
	public int getMinElements()
	{
		return minElements;
	}
	public void setMinElements(int minElements)
	{
		if (minElements < 0)
			throw new IllegalArgumentException("Argument can not be negative");
		this.minElements = minElements;
	}
	public int getMaxElements()
	{
		return maxElements;
	}
	public void setMaxElements(int maxElements)
	{
		if (maxElements < 0)
			throw new IllegalArgumentException("Argument can not be negative");
		this.maxElements = maxElements;
	}
	public boolean isSelfModificable()
	{
		return selfModificable;
	}
	public void setSelfModificable(boolean selfModificable)
	{
		this.selfModificable = selfModificable;
	}
	public AttributeVisibility getVisibility()
	{
		return visibility;
	}
	public void setVisibility(AttributeVisibility visibility)
	{
		if (visibility == null)
			throw new IllegalArgumentException("Argument can not be null");
		this.visibility = visibility;
	}
	public boolean isUniqueValues()
	{
		return uniqueValues;
	}

	public void setUniqueValues(boolean uniqueValues)
	{
		this.uniqueValues = uniqueValues;
	}

	public int getFlags()
	{
		return flags;
	}
	public void setFlags(int flags)
	{
		this.flags = flags;
	}

	@Override
	public void validateInitialization()
	{
		if (valueType == null)
			throw new IllegalAttributeTypeException("Attribute values type must be set for attribute type");
		if (maxElements < minElements)
			throw new IllegalAttributeTypeException("Max elements limit can not be less then min elements limit");
		if (name == null || name.trim().equals(""))
			throw new IllegalAttributeTypeException("Attribute type name must be set");
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		AttributeType other = (AttributeType) obj;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "AttributeType [description=" + description + ", name=" + name
				+ ", valueType=" + valueType + ", minElements=" + minElements
				+ ", maxElements=" + maxElements + ", uniqueValues=" + uniqueValues
				+ ", selfModificable=" + selfModificable + ", visibility="
				+ visibility + ", flags=" + flags + "]";
	}

}
