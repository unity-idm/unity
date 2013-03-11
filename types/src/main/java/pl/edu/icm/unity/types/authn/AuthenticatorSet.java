/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Collections;
import java.util.Set;

/**
 * Set of {@link Authenticator}s. The purpose of this set is to allow for
 * multi way authentication, i.e. if the set contains more then one {@link Authenticator}
 * then each of them must be used by a principal to have an overall authentication successful.
 * @author K. Benedyczak
 */
public class AuthenticatorSet
{
	private Set<String> authenticators;
	
	public AuthenticatorSet()
	{
		authenticators = Collections.emptySet();
	}
	
	public AuthenticatorSet(Set<String> authenticators)
	{
		this.authenticators = authenticators;
	}

	public Set<String> getAuthenticators()
	{
		return authenticators;
	}

	public void setAuthenticators(Set<String> authenticators)
	{
		this.authenticators = authenticators;
	}
}
