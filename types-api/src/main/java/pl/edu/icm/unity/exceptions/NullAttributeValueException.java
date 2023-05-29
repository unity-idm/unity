/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when attribtue value is null.
 */
public class NullAttributeValueException extends IllegalAttributeValueException
{
	public NullAttributeValueException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public NullAttributeValueException(String msg)
	{
		super(msg);
	}
}
