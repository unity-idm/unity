/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.Objects;

public class AttributeDefinitionWithMappingBean
{
	private AttributeDefinitionBean attributeDefinition;
	private AttributeMappingBean attributeMapping;

	public AttributeDefinitionWithMappingBean()
	{
		attributeDefinition = new AttributeDefinitionBean();
		attributeMapping = new AttributeMappingBean();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributeDefinition, attributeMapping);
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
		AttributeDefinitionWithMappingBean other = (AttributeDefinitionWithMappingBean) obj;
		return Objects.equals(attributeDefinition, other.attributeDefinition)
				&& Objects.equals(attributeMapping, other.attributeMapping);
	}

	public AttributeDefinitionBean getAttributeDefinition()
	{
		return attributeDefinition;
	}

	public void setAttributeDefinition(AttributeDefinitionBean attributeDefinition)
	{
		this.attributeDefinition = attributeDefinition;
	}

	public AttributeMappingBean getAttributeMapping()
	{
		return attributeMapping;
	}

	public void setAttributeMapping(AttributeMappingBean attributeMapping)
	{
		this.attributeMapping = attributeMapping;
	}

	@Override
	protected AttributeDefinitionWithMappingBean clone()
	{
		AttributeDefinitionWithMappingBean clone = new AttributeDefinitionWithMappingBean();
		clone.setAttributeDefinition(attributeDefinition.clone());
		clone.setAttributeMapping(attributeMapping.clone());
		return clone;
	}
}
