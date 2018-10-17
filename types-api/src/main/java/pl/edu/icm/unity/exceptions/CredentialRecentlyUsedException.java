/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when credential being set was recently used
 * 
 * @author K. Benedyczak
 */
public class CredentialRecentlyUsedException extends IllegalCredentialException
{
	public CredentialRecentlyUsedException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public CredentialRecentlyUsedException(String msg)
	{
		super(msg);
	}
}
