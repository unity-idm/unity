/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Configures the external sign up process during registration.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class ExternalSignupSpec
{
	private Set<String> specs;

	ExternalSignupSpec(Set<String> specs)
	{
		this.specs = specs;
	}

	ExternalSignupSpec()
	{
		specs = new HashSet<>();
	}

	@JsonCreator
	public ExternalSignupSpec(ObjectNode json)
	{
		fromJson(json);
	}

	private void fromJson(ObjectNode root)
	{
		try
		{
			ArrayNode specsNode = (ArrayNode) root.get("specs");
			specs = new HashSet<>(specsNode.size());
			for (int i = 0; i < specsNode.size(); i++)
			{
				specs.add(specsNode.get(i).asText());
			}
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize authentication flows spec from JSON", e);
		}
	}

	@JsonValue
	public ObjectNode toJsonObject()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		ArrayNode jsonSpecs = root.putArray("specs");
		specs.forEach(spec -> jsonSpecs.add(spec));
		return root;
	}

	public Set<String> getSpecs()
	{
		return specs;
	}

	public void setSpecs(Set<String> specs)
	{
		this.specs = specs;
	}
	
	public boolean isEnabled()
	{
		return !specs.isEmpty();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((specs == null) ? 0 : specs.hashCode());
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
		ExternalSignupSpec other = (ExternalSignupSpec) obj;
		if (specs == null)
		{
			if (other.specs != null)
				return false;
		} else if (!specs.equals(other.specs))
			return false;
		return true;
	}

	public static AuthenticationFlowsSpecBuilder builder()
	{
		return new AuthenticationFlowsSpecBuilder();
	}

	public static class AuthenticationFlowsSpecBuilder
	{
		private Set<String> specs = new HashSet<>();

		public AuthenticationFlowsSpecBuilder withSpecs(Set<String> specs)
		{
			this.specs = new HashSet<>(specs);
			return this;
		}

		public ExternalSignupSpec build()
		{
			return new ExternalSignupSpec(specs);
		}
	}
}
