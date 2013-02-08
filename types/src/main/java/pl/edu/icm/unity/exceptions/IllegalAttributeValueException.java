/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when attribtue value is invalid.
 * @author K. Benedyczak
 */
public class IllegalAttributeValueException extends RuntimeEngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalAttributeValueException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalAttributeValueException(String msg)
	{
		super(msg);
	}
}
