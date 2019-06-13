/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy.oauth;

class AuthenticationException extends RuntimeException
{
	AuthenticationException(String msg)
	{
		super(msg);
	}
	
	AuthenticationException(String msg, Exception cause)
	{
		super(msg, cause);
	}
}
