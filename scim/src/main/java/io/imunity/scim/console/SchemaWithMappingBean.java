/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.imunity.scim.config.SchemaType;

public class SchemaWithMappingBean
{
	private String id;
	private SchemaType type;
	private String name;
	private String description;
	private boolean enable;

	private List<AttributeDefinitionWithMappingBean> attributes;

	public SchemaWithMappingBean()
	{
		type = SchemaType.USER;
		attributes = new ArrayList<>();
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	protected SchemaWithMappingBean clone()
	{
		SchemaWithMappingBean clone = new SchemaWithMappingBean();
		clone.setAttributes(this.attributes.stream().map(a -> a.clone()).collect(Collectors.toList()));
		clone.setName(name);
		clone.setId(id);
		clone.setEnable(enable);
		clone.setDescription(description);
		clone.setType(type);
		return clone;
	}

	public List<AttributeDefinitionWithMappingBean> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(List<AttributeDefinitionWithMappingBean> attributes)
	{
		this.attributes = attributes;
	}

	public boolean isEnable()
	{
		return enable;
	}

	public boolean hasInvalidMappings()
	{
		for (AttributeDefinitionWithMappingBean bean : attributes)
		{
			if (bean != null)
			{
				if (!bean.inferAttributeNamesWithInvalidMapping().isEmpty())
					return true;
			}
		}
		
		return false;
	}
	public void setEnable(boolean enable)
	{
		this.enable = enable;
	}

	public SchemaType getType()
	{
		return type;
	}

	public void setType(SchemaType type)
	{
		this.type = type;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, description, enable, id, name, type);
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
		SchemaWithMappingBean other = (SchemaWithMappingBean) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(description, other.description)
				&& enable == other.enable && Objects.equals(id, other.id) && Objects.equals(name, other.name)
				&& Objects.equals(type, other.type);
	}

}
