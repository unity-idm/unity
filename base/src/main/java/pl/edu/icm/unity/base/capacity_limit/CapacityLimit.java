/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.capacity_limit;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.describedObject.NamedObject;

/**
 * Stores information about capacity limit.
 * 
 * @author P.Piernik
 *
 */
public class CapacityLimit implements NamedObject
{
	private CapacityLimitName name;
	private int value;

	@JsonCreator
	public CapacityLimit(ObjectNode root)
	{
		fromJson(root);
	}

	public CapacityLimit()
	{
	}

	public CapacityLimit(CapacityLimitName name, int value)
	{
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName()
	{
		return name.toString();
	}

	public void setName(CapacityLimitName name)
	{
		this.name = name;
	}

	public int getValue()
	{
		return value;
	}

	public void setValue(int value)
	{
		this.value = value;
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", getName());
		root.put("value", getValue());
		return root;
	}

	private void fromJson(ObjectNode root)
	{
		name = CapacityLimitName.valueOf(root.get("name").asText());
		value = root.get("value").asInt();
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final CapacityLimit that = (CapacityLimit) o;
		return name.equals(that.name) && value == that.value;

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, value);
	}
}
