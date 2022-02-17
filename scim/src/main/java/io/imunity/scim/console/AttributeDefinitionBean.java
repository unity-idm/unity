/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.ArrayList;
import java.util.List;

import io.imunity.scim.scheme.SCIMAttributeType;

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

}
