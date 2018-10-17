/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

public class UnexpectedIdentityException extends RuntimeException
{
	public final String expectedIdentity;

	public UnexpectedIdentityException(String expectedIdentity)
	{
		this.expectedIdentity = expectedIdentity;
	}
}
