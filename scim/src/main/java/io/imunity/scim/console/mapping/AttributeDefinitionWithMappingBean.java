/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.imunity.scim.schema.SCIMAttributeType;

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
		clone.setAttributeMapping(attributeMapping != null ? attributeMapping.clone() : null);
		return clone;
	}

	public List<String> inferAttributeNamesWithInvalidMapping()
	{
		if (attributeDefinition == null || attributeDefinition.getName().isEmpty())
			return Collections.emptyList();
		
		if (attributeMapping == null)
			return List.of(attributeDefinition.getName());
		if (attributeDefinition.isMultiValued())
		{
			if (attributeMapping.getDataArray() == null || attributeMapping.getDataArray().getType() == null)
			{
				return List.of(attributeDefinition.getName());
			}
		}

		if (!attributeDefinition.getType().equals(SCIMAttributeType.COMPLEX))
		{
			if (attributeDefinition.getType().equals(SCIMAttributeType.REFERENCE))
			{
				if (attributeMapping.getDataReference() == null || attributeMapping.getDataReference().getType() == null
						|| attributeMapping.getDataReference().getExpression() == null
						|| attributeMapping.getDataReference().getExpression().isEmpty())
					return List.of(attributeDefinition.getName());
			}

			else if (attributeMapping.getDataValue() == null || attributeMapping.getDataValue().getType() == null)
			{
				return List.of(attributeDefinition.getName());
			}
		}

		List<String> invalid = new ArrayList<>();
		for (AttributeDefinitionWithMappingBean sa : attributeDefinition.getSubAttributesWithMapping())
		{
			for (String a : sa.inferAttributeNamesWithInvalidMapping())
			{
				invalid.add(attributeDefinition.getName() + "." + a);
			}
		}

		return invalid;
	}
}
