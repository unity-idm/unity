/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;

/**
 * Configures the external sign up process during registration.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class ExternalSignupSpec
{
	private List<AuthenticationOptionsSelector> specs = new ArrayList<>();

	public ExternalSignupSpec(List<AuthenticationOptionsSelector> specs)
	{
		this.specs = specs;
	}
	
	ExternalSignupSpec() {} 

	public List<AuthenticationOptionsSelector> getSpecs()
	{
		return specs;
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
