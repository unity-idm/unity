/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

/**
 * OAuth request validation error
 * @author K. Benedyczak
 */
public class OAuthValidationException extends Exception
{
	public OAuthValidationException(String message)
	{
		super(message);
	}
}
