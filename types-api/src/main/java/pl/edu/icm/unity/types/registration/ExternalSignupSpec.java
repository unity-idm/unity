/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;

/**
 * Configures the external sign up process during registration.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class ExternalSignupSpec
{
	private List<AuthenticationOptionKey> specs;
	private String userExistsRedirectUrl;

	ExternalSignupSpec(List<AuthenticationOptionKey> specs, String userExistsRedirectUrl)
	{
		this.specs = specs;
		this.userExistsRedirectUrl = userExistsRedirectUrl;
	}

	ExternalSignupSpec()
	{
		specs = new ArrayList<>();
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
			if (JsonUtil.notNull(root, "specs"))
			{
				ArrayNode specsNode = (ArrayNode) root.get("specs");
				specs = new ArrayList<>(specsNode.size());
				for (int i = 0; i < specsNode.size(); i++)
				{
					specs.add(new AuthenticationOptionKey((ObjectNode) specsNode.get(i)));
				}
			}
			if (JsonUtil.notNull(root, "userExistsRedirectUrl"))
				userExistsRedirectUrl = root.get("userExistsRedirectUrl").asText();
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
		specs.forEach(spec -> jsonSpecs.add(spec.toJsonObject()));
		root.put("userExistsRedirectUrl", userExistsRedirectUrl);
		return root;
	}
	
	public List<AuthenticationOptionKey> getSpecs()
	{
		return specs;
	}

	public void setSpecs(List<AuthenticationOptionKey> specs)
	{
		this.specs = specs;
	}
	
	public String getUserExistsRedirectUrl()
	{
		return userExistsRedirectUrl;
	}

	public void setUserExistsRedirectUrl(String userExistsRedirectUrl)
	{
		this.userExistsRedirectUrl = userExistsRedirectUrl;
	}

	public boolean isEnabled()
	{
		return !specs.isEmpty();
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof ExternalSignupSpec))
			return false;
		ExternalSignupSpec castOther = (ExternalSignupSpec) other;
		return Objects.equals(specs, castOther.specs)
				&& Objects.equals(userExistsRedirectUrl, castOther.userExistsRedirectUrl);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(specs, userExistsRedirectUrl);
	}

	public static ExternalSignupSpecBuilder builder()
	{
		return new ExternalSignupSpecBuilder();
	}

	public static class ExternalSignupSpecBuilder
	{
		private List<AuthenticationOptionKey> specs = new ArrayList<>();
		private String userExistsRedirectUrl;

		public ExternalSignupSpecBuilder withSpecs(Set<AuthenticationOptionKey> specs)
		{
			this.specs = new ArrayList<>(specs);
			return this;
		}
		
		public ExternalSignupSpecBuilder withSpecs(AuthenticationOptionKey... specs)
		{
			this.specs = new ArrayList<>();
			for (AuthenticationOptionKey spec : specs)
				this.specs.add(spec);
			return this;
		}
		
		public ExternalSignupSpecBuilder withUserExistsRedirectUrl(String userExistsRedirectUrl)
		{
			this.userExistsRedirectUrl = userExistsRedirectUrl;
			return this;
		}

		public ExternalSignupSpec build()
		{
			return new ExternalSignupSpec(specs, userExistsRedirectUrl);
		}
	}
}
