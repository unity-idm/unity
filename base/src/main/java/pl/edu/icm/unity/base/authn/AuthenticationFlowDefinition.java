/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.authn;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import pl.edu.icm.unity.base.describedObject.NamedObject;

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
		REQUIRE, USER_OPTIN, NEVER, DYNAMIC
	}

	private String name;
	private Set<String> firstFactorAuthenticators;
	private List<String> secondFactorAuthenticators;
	private Policy policy;
	private String dynamicPolicyMvelCondition;
	private long revision = 0;
	

	public AuthenticationFlowDefinition()
	{
	}
	
	public AuthenticationFlowDefinition(String name, Policy policy,
			Set<String> firstFactorAuthenticators,
			List<String> secondFactorAuthenticators, String dynamicPolicyMvelCondition)
	{
		this.name = name;
		this.firstFactorAuthenticators = firstFactorAuthenticators;
		this.secondFactorAuthenticators = secondFactorAuthenticators;
		this.policy = policy;
		this.dynamicPolicyMvelCondition = dynamicPolicyMvelCondition;
	}
	
	public AuthenticationFlowDefinition(String name, Policy policy,
			Set<String> firstFactorAuthenticators)
	{
		this(name, policy, firstFactorAuthenticators, new ArrayList<>(), null);
	}
	
	@JsonIgnore
	public Set<String> getAllAuthenticators()
	{
		Set<String> ret = new LinkedHashSet<>();
		ret.addAll(firstFactorAuthenticators);
		ret.addAll(secondFactorAuthenticators);
		return ret;	
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
	
	public long getRevision()
	{
		return revision;
	}

	public void setRevision(long revision)
	{
		this.revision = revision;
	}
	
	public String getDynamicPolicyMvelCondition()
	{
		return dynamicPolicyMvelCondition;
	}

	public void setDynamicPolicyMvelCondition(String configuration)
	{
		this.dynamicPolicyMvelCondition = configuration;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(dynamicPolicyMvelCondition, firstFactorAuthenticators, name, policy, revision,
				secondFactorAuthenticators);
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
		AuthenticationFlowDefinition other = (AuthenticationFlowDefinition) obj;
		return Objects.equals(dynamicPolicyMvelCondition, other.dynamicPolicyMvelCondition)
				&& Objects.equals(firstFactorAuthenticators, other.firstFactorAuthenticators)
				&& Objects.equals(name, other.name) && policy == other.policy && revision == other.revision
				&& Objects.equals(secondFactorAuthenticators, other.secondFactorAuthenticators);
	}
	
	
}
