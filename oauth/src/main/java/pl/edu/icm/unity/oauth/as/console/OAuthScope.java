/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import java.util.List;
import java.util.Objects;

/**
 * Represent single oauth scope.
 * 
 * @author P.Piernik
 *
 */
public class OAuthScope
{
	private String name;
	private String description;
	private List<String> attributes;

	public OAuthScope()
	{
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

	public List<String> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(List<String> attributes)
	{
		this.attributes = attributes;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, description, name);
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
		OAuthScope other = (OAuthScope) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(description, other.description)
				&& Objects.equals(name, other.name);
	}
	
	
}