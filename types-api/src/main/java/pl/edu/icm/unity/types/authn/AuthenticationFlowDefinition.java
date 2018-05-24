/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.authn;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.types.NamedObject;

/**
 * Configuration of a authentication flow. Contains first and second factor
 * authenticators and policy which defines how the second factor should be
 * enforced.
 * 
 * @author P.Piernik
 *
 */
public class AuthenticationFlowDefinition implements NamedObject
{
	public enum Policy
	{
		REQUIRE, USER_OPTIN, NEVER, RISK_BASED
	}

	private String name;
	private Set<String> firstFactorAuthenticators;
	private List<String> secondFactorAuthenticators;
	private Policy policy;

	public AuthenticationFlowDefinition()
	{
		super();
	}

	public AuthenticationFlowDefinition(String name, Policy policy,
			Set<String> firstFactorAuthenticators,
			List<String> secondFactorAuthenticators)
	{
		this.name = name;
		this.firstFactorAuthenticators = firstFactorAuthenticators;
		this.secondFactorAuthenticators = secondFactorAuthenticators;
		this.policy = policy;
	}

	public Set<String> getFirstFactorAuthenticators()
	{
		return firstFactorAuthenticators;
	}

	public void setFirstFactorAuthenticators(Set<String> firstFactorAuthenticators)
	{
		this.firstFactorAuthenticators = firstFactorAuthenticators;
	}

	public List<String> getSecondFactorAuthenticators()
	{
		return secondFactorAuthenticators;
	}

	public void setSecondFactorAuthenticators(List<String> secondFactorAuthenticators)
	{
		this.secondFactorAuthenticators = secondFactorAuthenticators;
	}

	public Policy getPolicy()
	{
		return policy;
	}

	public void setPolicy(Policy policy)
	{
		this.policy = policy;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}
}
