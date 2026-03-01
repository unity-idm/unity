/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.json.JsonUtil;

/**
 * Represents an attribute instance.
 * Attribute has a group where it is valid (or valid and defined depending on context) and list of values.
 * Values type of this class are of String class. While for String attributes this is a natural encoding
 * for some other the string is an effect of some sort of serialization as Base64. Values can be (de)serialized using
 * proper value syntax object. The value syntax name is stored with an attribute for convenience, although it
 * duplicates an information stored in {@link AttributeType} of this attribute.
 * <p>
 * @author K. Benedyczak
 */
public class Attribute implements NamedObject
{
	private String name;
	private String valueSyntax;
	private String groupPath;
	private List<String> values = Collections.emptyList();
	private String translationProfile;
	private String remoteIdp;

	public Attribute(String name, String valueSyntax, String groupPath, List<String> values)
	{
		this.name = name;
		this.valueSyntax = valueSyntax;
		this.groupPath = groupPath;
		this.values = new ArrayList<>(values);
	}

	/**
	 * Full constructor
	 */
	public Attribute(String name, String valueSyntax, String groupPath, List<String> values,
			String remoteIdp, String translationProfile)
	{
		this(name, valueSyntax, groupPath, values);
		this.remoteIdp = remoteIdp;
		this.translationProfile = translationProfile;
	}

	public Attribute(Attribute toClone)
	{
		this(toClone.name, toClone.valueSyntax, toClone.groupPath, toClone.values, toClone.remoteIdp, 
				toClone.translationProfile);
	}
	
	/**
	 * Full deserialization from JSON
	 */
	@JsonCreator
	public Attribute(ObjectNode src)
	{
		fromJson(src);
	}

	/**
	 * Partial deserialization from JSON
	 */
	public Attribute(String name, String valueSyntax, String groupPath, ObjectNode src)
	{
		this.name = name;
		this.valueSyntax = valueSyntax;
		this.groupPath = groupPath;
		fromJsonBase(src);
	}

	public String getGroupPath()
	{
		return groupPath;
	}

	public List<String> getValues()
	{
		return values;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public String getTranslationProfile()
	{
		return translationProfile;
	}

	public String getRemoteIdp()
	{
		return remoteIdp;
	}

	public String getValueSyntax()
	{
		return valueSyntax;
	}


	public void setName(String name)
	{
		this.name = name;
	}

	public void setValueSyntax(String valueSyntax)
	{
		this.valueSyntax = valueSyntax;
	}

	public void setGroupPath(String groupPath)
	{
		this.groupPath = groupPath;
	}

	@JsonSetter
	public void setValues(List<String> values)
	{
		this.values = values;
	}

	public void setValues(String... values)
	{
		List<String> valuesL = new ArrayList<>(values.length);
		Collections.addAll(valuesL, values);
		setValues(valuesL);
	}

	public void setTranslationProfile(String translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	public void setRemoteIdp(String remoteIdp)
	{
		this.remoteIdp = remoteIdp;
	}


	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = toJsonBase();
		root.put("name", getName());
		root.put("groupPath", getGroupPath());
		root.put("valueSyntax", valueSyntax);
		return root;
	}


	protected final void fromJson(ObjectNode main)
	{
		this.name = main.get("name").asText();
		this.groupPath = main.get("groupPath").asText();
		JsonNode vSyntax = main.get("valueSyntax");
		if (vSyntax != null && !vSyntax.isNull())
			this.valueSyntax = vSyntax.asText();
		fromJsonBase(main);
	}

	protected final void fromJsonBase(ObjectNode main)
	{
		translationProfile = JsonUtil.getNullable(main, "translationProfile");
		remoteIdp = JsonUtil.getNullable(main, "remoteIdp");

		ArrayNode values = main.withArray("values");
		this.values = new ArrayList<>(values.size());
		Iterator<JsonNode> it = values.iterator();
		try
		{
			while(it.hasNext())
				this.values.add(it.next().asText());
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}

	protected ObjectNode toJsonBase()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		if (getRemoteIdp() != null)
			root.put("remoteIdp", getRemoteIdp());
		if (getTranslationProfile() != null)
			root.put("translationProfile", getTranslationProfile());
		ArrayNode values = root.putArray("values");
		for (String value: getValues())
			values.add(value);
		return root;
	}

	@Override
	public Attribute clone()
	{
		return new Attribute(toJson());
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("[").append(groupPath).append("]").append(": [");
		int size = values.size();
		for (int i=0; i<size; i++)
		{
			sb.append(values.get(i));
			if (i<size-1)
				sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(groupPath, name, remoteIdp, translationProfile, valueSyntax, values);
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
		Attribute other = (Attribute) obj;
		return Objects.equals(groupPath, other.groupPath) && Objects.equals(name, other.name)
				&& Objects.equals(remoteIdp, other.remoteIdp)
				&& Objects.equals(translationProfile, other.translationProfile)
				&& Objects.equals(valueSyntax, other.valueSyntax) && Objects.equals(values, other.values);
	}
}
