/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Objects;

/**
 * Describes expected identity
 * 
 * @author K. Benedyczak
 */
public class ExpectedIdentity
{
	public enum IdentityExpectation
	{
		HINT,
		MANDATORY
	}
	
	private String identity;
	private IdentityExpectation expectation;

	public ExpectedIdentity(String identity, IdentityExpectation expectation)
	{
		this.identity = identity;
		this.expectation = expectation;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(expectation, identity);
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
		ExpectedIdentity other = (ExpectedIdentity) obj;
		return expectation == other.expectation && Objects.equals(identity, other.identity);
	}



	//for JSON
	protected ExpectedIdentity()
	{
	}
	
	public String getIdentity()
	{
		return identity;
	}

	public IdentityExpectation getExpectation()
	{
		return expectation;
	}

	@Override
	public String toString()
	{
		return identity + " (" + expectation + ")";
	}
}