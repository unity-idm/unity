/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;


class AuthenticationFlowDefinitionForBinder
{
	private String name;
	private Set<String> firstFactorAuthenticators;
	private List<String> secondFactorAuthenticators;
	private Policy policy;
	private String policyConfiguration;
	
	AuthenticationFlowDefinitionForBinder(String name,Policy policy, Set<String> firstFactorAuthenticators,
			List<String> secondFactorAuthenticators, String policyConfiguration)
	{
		this.name = name;
		this.firstFactorAuthenticators = firstFactorAuthenticators;
		this.secondFactorAuthenticators = secondFactorAuthenticators;
		this.policy = policy;
		this.policyConfiguration = policyConfiguration;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
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

	public String getPolicyConfiguration()
	{
		return policyConfiguration;
	}

	public void setPolicyConfiguration(String policyConfiguration)
	{
		this.policyConfiguration = policyConfiguration;
	}
}
