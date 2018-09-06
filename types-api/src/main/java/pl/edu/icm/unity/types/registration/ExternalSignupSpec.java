/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;

/**
 * Configures the external sign up process during registration.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class ExternalSignupSpec
{
	private List<AuthenticationOptionKey> specs = new ArrayList<>();
	private String userExistsRedirectUrl;

	ExternalSignupSpec(List<AuthenticationOptionKey> specs, String userExistsRedirectUrl)
	{
		this.specs = specs;
		this.userExistsRedirectUrl = userExistsRedirectUrl;
	}
	
	ExternalSignupSpec() {} 

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

	@JsonIgnore
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

		public ExternalSignupSpecBuilder withSpecs(List<AuthenticationOptionKey> specs)
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
