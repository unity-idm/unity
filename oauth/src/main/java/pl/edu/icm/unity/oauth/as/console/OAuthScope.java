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
	private boolean enabled;
	private String description;
	private List<String> attributes;

	public OAuthScope(String name, String desc)
	{
		this.name = name;
		this.description = desc;
	}
	
	public OAuthScope()
	{
		enabled = true;
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
		return Objects.hash(attributes, description, enabled, name);
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	@Override
	protected OAuthScope clone() 
	{
		OAuthScope clone = new OAuthScope();
		clone.setName(getName());
		clone.setAttributes(getAttributes());
		clone.setEnabled(isEnabled());
		clone.setDescription(getDescription());
		return clone;
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
				&& enabled == other.enabled && Objects.equals(name, other.name);
	}
}