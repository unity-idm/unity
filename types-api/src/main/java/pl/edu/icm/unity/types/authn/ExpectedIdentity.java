/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

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