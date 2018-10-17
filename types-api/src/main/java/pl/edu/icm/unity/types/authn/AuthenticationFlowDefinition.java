/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.authn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
		REQUIRE, USER_OPTIN, NEVER
	}

	private String name;
	private Set<String> firstFactorAuthenticators;
	private List<String> secondFactorAuthenticators;
	private Policy policy;
	private long revision = 0;
	

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
	
	public AuthenticationFlowDefinition(String name, Policy policy,
			Set<String> firstFactorAuthenticators)
	{
		this(name, policy, firstFactorAuthenticators, new ArrayList<>());
	}
	
	@JsonIgnore
	public Set<String> getAllAuthenticators()
	{
		Set<String> ret = new HashSet<>();
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
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((policy == null) ? 0 : policy.hashCode());
		result = prime * result + ((firstFactorAuthenticators == null) ? 0
				: firstFactorAuthenticators.hashCode());
		result = prime * result + ((secondFactorAuthenticators == null) ? 0
				: secondFactorAuthenticators.hashCode());
		result = prime * result + (int) (revision ^ (revision >>> 32));
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
		AuthenticationFlowDefinition other = (AuthenticationFlowDefinition) obj;
		
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		
		if (policy == null)
		{
			if (other.policy != null)
				return false;
		} else if (!policy.equals(other.policy))
			return false;
		
		if (secondFactorAuthenticators == null)
		{
			if (other.secondFactorAuthenticators != null)
				return false;
		} else if (!secondFactorAuthenticators.equals(other.secondFactorAuthenticators))
			return false;
		if (firstFactorAuthenticators == null)
		{
			if (other.firstFactorAuthenticators != null)
				return false;
		} else if (!firstFactorAuthenticators.equals(other.firstFactorAuthenticators))
			return false;
		if (revision != other.revision)
			return false;
		return true;
	}
}
