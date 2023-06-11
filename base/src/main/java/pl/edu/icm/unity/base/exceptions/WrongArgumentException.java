/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.exceptions;

/**
 * Thrown when there is problem with arguments, and when we want to have a checked exception.
 * Otherwise the unchecked {@link IllegalArgumentException} should be thrown.
 * @author K. Benedyczak
 */
public class WrongArgumentException extends EngineException
{
	public WrongArgumentException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public WrongArgumentException(String msg)
	{
		super(msg);
	}
}
