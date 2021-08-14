/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

class UnexpectedIdentityException extends RuntimeException
{
	final String expectedIdentity;

	UnexpectedIdentityException(String expectedIdentity)
	{
		this.expectedIdentity = expectedIdentity;
	}
}
