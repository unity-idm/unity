/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.List;
import java.util.Objects;

import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;

/**
 * Configures with remote signUp methods should be shown in grid. Additionally, it determines whether search field should be shown
 * @author P.Piernik
 *
 */
public class ExternalSignupGridSpec extends ExternalSignupSpec
{
	private boolean searchable;

	public ExternalSignupGridSpec(List<AuthenticationOptionKey> specs, boolean searchable)
	{
		super(specs);
		this.searchable = searchable;
	}
	
	ExternalSignupGridSpec() {} 

	
	public boolean isSearchable()
	{
		return searchable;
	}

	public void setSearchable(boolean searchable)
	{
		this.searchable = searchable;
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof ExternalSignupGridSpec))
			return false;
		if (!super.equals(other))
			return false;
		
		ExternalSignupGridSpec castOther = (ExternalSignupGridSpec) other;
		return Objects.equals(searchable, castOther.searchable);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), searchable);
	}
}
