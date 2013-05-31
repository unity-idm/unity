/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when credential definition is invalid.
 * @author K. Benedyczak
 */
public class IllegalCredentialException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalCredentialException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalCredentialException(String msg)
	{
		super(msg);
	}
}
