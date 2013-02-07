/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Attribute type defines rules for handling attributes. Particular values
 * are subject to constraints defined in {@link AttributeValueSyntax} interface, 
 * with pluggable implementation. This class adds universal functionality:
 * descriptions, values cardinality limits and more.
 */
public class AttributeType<T>
{
	/**
	 * The attribute type can not be created or changed using management API (it is created
	 * internally).
	 */
	public static final int TYPE_IMMUTABLE_FLAG = 0x01;
	
	/**
	 * The attribute type instances can not be created, updated or removed using management API
	 * (there are specialized methods to manipulate such attributes).  
	 */
	public static final int INSTANCES_IMMUTABLE_FLAG = 0x02;
	
	/**
	 * The attribute instance defined in a group must contain all values that are defined for the
	 * bearer in the closest parent group. Especially useful for authZ attributes, to forbid
	 * limiting of permissions in subgroups. 
	 */
	public static final int NO_VALUES_LIMITING_FLAG = 0x04;
	
	private String description;
	private String name;
	private AttributeValueSyntax<T> valueType;
	private int minElements;
	private int maxElements;
	private boolean selfModificable;
	private AttributeVisibility visibility;
	private int flags;
	
	
	
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public AttributeValueSyntax<T> getValueType()
	{
		return valueType;
	}
	public void setValueType(AttributeValueSyntax<T> valueType)
	{
		this.valueType = valueType;
	}
	public int getMinElements()
	{
		return minElements;
	}
	public void setMinElements(int minElements)
	{
		this.minElements = minElements;
	}
	public int getMaxElements()
	{
		return maxElements;
	}
	public void setMaxElements(int maxElements)
	{
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
		this.visibility = visibility;
	}
	public int getFlags()
	{
		return flags;
	}
	public void setFlags(int flags)
	{
		this.flags = flags;
	}
}
