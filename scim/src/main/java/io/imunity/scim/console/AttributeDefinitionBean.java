/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.imunity.scim.schema.SCIMAttributeType;

public class AttributeDefinitionBean
{
	private String name;
	private SCIMAttributeType type;
	private String description;
	private List<AttributeDefinitionWithMappingBean> subAttributesWithMapping;
	private boolean multiValued;

	public AttributeDefinitionBean()
	{
		subAttributesWithMapping = new ArrayList<>();
		type = SCIMAttributeType.STRING;
		name = new String();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public SCIMAttributeType getType()
	{
		return type;
	}

	public void setType(SCIMAttributeType type)
	{
		this.type = type;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public boolean isMultiValued()
	{
		return multiValued;
	}

	public void setMultiValued(boolean multiValued)
	{
		this.multiValued = multiValued;
	}

	public List<AttributeDefinitionWithMappingBean> getSubAttributesWithMapping()
	{
		return subAttributesWithMapping;
	}

	public void setSubAttributesWithMapping(List<AttributeDefinitionWithMappingBean> subAttributesWithMapping)
	{
		this.subAttributesWithMapping = subAttributesWithMapping;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(description, multiValued, name, subAttributesWithMapping, type);
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
		AttributeDefinitionBean other = (AttributeDefinitionBean) obj;
		return Objects.equals(description, other.description) && multiValued == other.multiValued
				&& Objects.equals(name, other.name)
				&& Objects.equals(subAttributesWithMapping, other.subAttributesWithMapping) && type == other.type;
	}

	@Override
	protected AttributeDefinitionBean clone()
	{
		AttributeDefinitionBean clone = new AttributeDefinitionBean();
		clone.setName(name);
		clone.setMultiValued(multiValued);
		clone.setType(type);
		clone.setDescription(description);
		clone.setSubAttributesWithMapping(
				subAttributesWithMapping.stream().map(a -> a.clone()).collect(Collectors.toList()));
		return clone;
	}

}
