/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.types.I18nDescribedObject;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.InitializationValidator;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Attribute type defines rules for handling attributes. Particular values
 * are subject to constraints defined in {@link AttributeValueSyntax} interface, 
 * with pluggable implementation. This class adds universal functionality:
 * descriptions, values cardinality limits and more.
 * <p>
 * The class is compared only using the name.
 */
public class AttributeType extends I18nDescribedObject implements InitializationValidator, NamedObject
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
	
	private String name;
	private AttributeValueSyntax<?> valueType;
	private int minElements = 0;
	private int maxElements = 1;
	private boolean uniqueValues = false;
	private boolean selfModificable = false;
	private AttributeVisibility visibility = AttributeVisibility.full;
	private int flags = 0;
	private Map<String, String> metadata = new HashMap<>();
	
	
	public AttributeType()
	{
	}
	
	public AttributeType(String name, AttributeValueSyntax<?> syntax)
	{
		this.name = name;
		this.valueType = syntax;
		this.displayedName = new I18nString(name);
	}
	
	public AttributeType(String name, AttributeValueSyntax<?> syntax, I18nString displayedName, I18nString description)
	{
		this(name, syntax);
		this.description = description;
		this.displayedName = displayedName;
	}

	/**
	 * This version resolves the descriptions of the attribute from the message bundles. The key must be
	 * AttrType.ATTR_NAME.desc.
	 * @param name
	 * @param syntax
	 * @param msg
	 */
	public AttributeType(String name, AttributeValueSyntax<?> syntax, MessageSource msg)
	{
		this(name, syntax, loadNames(name, msg), loadDescriptions(name, msg));
	}
	
	/**
	 * This version resolves the descriptions of the attribute from the message bundles. The key must be
	 * AttrType.msgKey.desc. It is possible to provide message arguments
	 * @param name
	 * @param syntax
	 * @param msg
	 */
	public AttributeType(String name, AttributeValueSyntax<?> syntax, MessageSource msg, String msgKey, 
			Object[] args)
	{
		this(name, syntax, loadNames(name, msg), loadDescriptions(msgKey, msg, args));
	}
	
	private static I18nString loadDescriptions(String msgKey, MessageSource msg, Object... args)
	{
		return new I18nString("AttrType." + msgKey + ".desc", msg, args);
	}

	private static I18nString loadNames(String msgKey, MessageSource msg, Object... args)
	{
		return new I18nString("AttrType." + msgKey + ".displayedName", msg, args);
	}
	
	public boolean isTypeImmutable()
	{
		return (flags & TYPE_IMMUTABLE_FLAG) != 0;
	}
	
	public boolean isInstanceImmutable()
	{
		return (flags & INSTANCES_IMMUTABLE_FLAG) != 0;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		if (name == null)
			throw new IllegalArgumentException("Argument can not be null");
		this.name = name;
		if (displayedName == null)
			displayedName = new I18nString(name);
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
	public void validateInitialization() throws IllegalAttributeTypeException
	{
		if (valueType == null)
			throw new IllegalAttributeTypeException("Attribute values type must be set for attribute type");
		if (maxElements < minElements)
			throw new IllegalAttributeTypeException("Max elements limit can not be less then min elements limit");
		if (name == null || name.trim().equals(""))
			throw new IllegalAttributeTypeException("Attribute type name must be set");
		if (displayedName == null)
			throw new IllegalAttributeTypeException("Attribute displayed name must be set");
	}

	public Map<String, String> getMetadata()
	{
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata)
	{
		this.metadata = metadata;
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
