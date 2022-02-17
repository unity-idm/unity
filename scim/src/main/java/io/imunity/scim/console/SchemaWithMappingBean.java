/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SchemaWithMappingBean
{
	private String id;
	private String name;
	private String description;
	private boolean enable;

	private List<AttributeDefinitionWithMappingBean> attributes;

	public SchemaWithMappingBean()
	{
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
		clone.setAttributes(this.attributes);
		clone.setName(name);
		clone.setId(id);
		clone.setEnable(enable);
		clone.setDescription(description);
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

	public void setEnable(boolean enable)
	{
		this.enable = enable;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, description, enable, id, name);
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
				&& enable == other.enable && Objects.equals(id, other.id) && Objects.equals(name, other.name);
	}

}
