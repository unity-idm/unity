/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when a previous credential is incorrect, upon credential change
 * @author K. Benedyczak
 */
public class IllegalPreviousCredentialException extends IllegalCredentialException
{
	private static final long serialVersionUID = 1L;

	public IllegalPreviousCredentialException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalPreviousCredentialException(String msg)
	{
		super(msg);
	}
}
