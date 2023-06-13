/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.describedObject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;

/**
 * Read only implementation of {@link DescribedObject} interface. Useful for extending
 * @author K. Benedyczak
 */
public class DescribedObjectROImpl implements DescribedObject
{
	protected String name;
	protected String description;
	
	protected DescribedObjectROImpl()
	{
	}
	
	public DescribedObjectROImpl(String name, String description)
	{
		this.name = name;
		this.description = description;
	}
	
	public DescribedObjectROImpl(ObjectNode root)
	{
		fromJsonDescObj(root);
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String toString()
	{
		return "DescribedObject [name=" + name + ", description=" + description + "]";
	}

	protected final void fromJsonDescObj(ObjectNode root)
	{
		description = root.get("description").asText(null);
		name = root.get("name").asText();
	}
	
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("description", getDescription());
		root.put("name", getName());
		return root;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
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
		DescribedObjectROImpl other = (DescribedObjectROImpl) obj;
		if (description == null)
		{
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
