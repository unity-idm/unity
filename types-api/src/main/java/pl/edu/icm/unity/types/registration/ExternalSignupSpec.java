/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;

/**
 * Configures the external sign up process during registration.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@JsonIgnoreProperties(ignoreUnknown = true) //TODO remove before release
public class ExternalSignupSpec
{
	private List<AuthenticationOptionKey> specs = new ArrayList<>();

	public ExternalSignupSpec(List<AuthenticationOptionKey> specs)
	{
		this.specs = specs;
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
		return Objects.equals(specs, castOther.specs);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(specs);
	}
}
