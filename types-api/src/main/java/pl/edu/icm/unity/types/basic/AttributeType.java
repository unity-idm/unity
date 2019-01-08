/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.types.I18nDescribedObject;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.InitializationValidator;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Attribute type defines rules for handling attributes. This class provides universal configuration:
 * descriptions, values cardinality limits and more.
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
	private String valueSyntax;
	private JsonNode valueSyntaxConfiguration;
	private int minElements = 0;
	private int maxElements = 1;
	private boolean uniqueValues = false;
	private boolean selfModificable = false;
	private int flags = 0;
	private Map<String, String> metadata = new HashMap<>();
	
	
	public AttributeType()
	{
	}
	
	public AttributeType(String name, String valueSyntax)
	{
		this.name = name;
		this.valueSyntax = valueSyntax;
		this.displayedName = new I18nString(name);
	}
	
	public AttributeType(String name, String valueSyntax, I18nString displayedName, I18nString description)
	{
		this(name, valueSyntax);
		this.description = description;
		this.displayedName = displayedName;
		if (displayedName.getDefaultValue() == null)
			displayedName.setDefaultValue(name);
	}

	@JsonCreator
	public AttributeType(ObjectNode root)
	{
		fromJson(root);
	}

	/**
	 * This version resolves the descriptions of the attribute from the message bundles. The key must be
	 * AttrType.ATTR_NAME.desc.
	 * @param name
	 * @param syntax
	 * @param msg
	 */
	public AttributeType(String name, String valueSyntax, MessageSource msg)
	{
		this(name, valueSyntax, loadNames(name, msg), loadDescriptions(name, msg));
	}
	
	/**
	 * This version resolves the descriptions of the attribute from the message bundles. The key must be
	 * AttrType.msgKey.desc. It is possible to provide message arguments
	 * @param name
	 * @param syntax
	 * @param msg
	 */
	public AttributeType(String name, String valueSyntax, MessageSource msg, String msgKey, 
			Object[] args)
	{
		this(name, valueSyntax, loadNames(name, msg), loadDescriptions(msgKey, msg, args));
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
	
	public String getValueSyntax()
	{
		return valueSyntax;
	}
	public void setValueSyntax(String valueSyntax)
	{
		if (valueSyntax == null)
			throw new IllegalArgumentException("Argument can not be null");
		this.valueSyntax = valueSyntax;
	}
	public JsonNode getValueSyntaxConfiguration()
	{
		return valueSyntaxConfiguration;
	}
	public void setValueSyntaxConfiguration(JsonNode valueSyntaxConfiguration)
	{
		this.valueSyntaxConfiguration = valueSyntaxConfiguration;
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
		if (valueSyntax == null)
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

	/**
	 * Serializes to JSON without syntax ID and name
	 * @return
	 */
	public ObjectNode toJsonBase()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("flags", getFlags());
		root.put("maxElements", getMaxElements());
		root.put("minElements", getMinElements());
		root.put("selfModificable", isSelfModificable());
		root.put("uniqueValues", isUniqueValues());
		root.set("syntaxState", getValueSyntaxConfiguration());
		root.set("displayedName", I18nStringJsonUtil.toJson(getDisplayedName()));
		root.set("i18nDescription", I18nStringJsonUtil.toJson(getDescription()));
		ObjectNode metaN = root.putObject("metadata");
		for (Map.Entry<String, String> entry: getMetadata().entrySet())
			metaN.put(entry.getKey(), entry.getValue());
		return root;
	}
	
	/**
	 * As {@link #toJsonBase(AttributeType)} but also adds information about attribute type name and syntax
	 * @param src
	 * @return
	 */
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = toJsonBase();
		root.put("name", getName());
		root.put("syntaxId", getValueSyntax());
		return root;
	}

	/**
	 * Initializes base state from JSON everything besides name and syntax
	 * @param main
	 * @param target
	 */
	public void fromJsonBase(ObjectNode main)
	{
		setFlags(main.get("flags").asInt());
		setMaxElements(main.get("maxElements").asInt());
		setMinElements(main.get("minElements").asInt());
		setSelfModificable(main.get("selfModificable").asBoolean());
		setUniqueValues(main.get("uniqueValues").asBoolean());
		if (JsonUtil.notNull(main, "syntaxState"))
			setValueSyntaxConfiguration((ObjectNode) main.get("syntaxState"));
		setDisplayedName(I18nStringJsonUtil.fromJson(main.get("displayedName")));
		if (getDisplayedName().getDefaultValue() == null)
			getDisplayedName().setDefaultValue(getName());
		setDescription(I18nStringJsonUtil.fromJson(main.get("i18nDescription"), 
				main.get("description")));
		if (main.has("metadata"))
		{
			JsonNode metaNode = main.get("metadata");
			Iterator<Entry<String, JsonNode>> it = metaNode.fields();
			Map<String, String> meta = getMetadata();
			while(it.hasNext())
			{
				Entry<String, JsonNode> entry = it.next();
				meta.put(entry.getKey(), entry.getValue().asText());
			}	
		}
	}

	/**
	 * Complete JSON deserialization
	 * @param main
	 */
	private void fromJson(ObjectNode main) 
	{
		setName(main.get("name").asText());
		setValueSyntax(main.get("syntaxId").asText());
		fromJsonBase(main);
	}

	@Override
	public AttributeType clone()
	{
		ObjectNode json = toJson();
		return new AttributeType(json);
	}
	
	@Override
	public String toString()
	{
		return "AttributeType [description=" + description + ", name=" + name
				+ ", valueSyntax=" + valueSyntax + ", valueSyntaxConfig="
				+ valueSyntaxConfiguration + ", minElements=" + minElements
				+ ", maxElements=" + maxElements + ", uniqueValues=" + uniqueValues
				+ ", selfModificable=" + selfModificable + ", flags=" + flags + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + flags;
		result = prime * result + maxElements;
		result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
		result = prime * result + minElements;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (selfModificable ? 1231 : 1237);
		result = prime * result + (uniqueValues ? 1231 : 1237);
		result = prime * result + ((valueSyntax == null) ? 0 : valueSyntax.hashCode());
		result = prime * result + ((valueSyntaxConfiguration == null) ? 0
				: valueSyntaxConfiguration.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeType other = (AttributeType) obj;
		if (flags != other.flags)
			return false;
		if (maxElements != other.maxElements)
			return false;
		if (metadata == null)
		{
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
			return false;
		if (minElements != other.minElements)
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (selfModificable != other.selfModificable)
			return false;
		if (uniqueValues != other.uniqueValues)
			return false;
		if (valueSyntax == null)
		{
			if (other.valueSyntax != null)
				return false;
		} else if (!valueSyntax.equals(other.valueSyntax))
			return false;
		if (valueSyntaxConfiguration == null)
		{
			if (other.valueSyntaxConfiguration != null)
				return false;
		} else if (!valueSyntaxConfiguration.equals(other.valueSyntaxConfiguration))
			return false;
		return true;
	}
}
