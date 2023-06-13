/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.identity;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Thrown when identity value is invalid.
 * @author K. Benedyczak
 */
public class IllegalIdentityValueException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalIdentityValueException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalIdentityValueException(String msg)
	{
		super(msg);
	}
}
