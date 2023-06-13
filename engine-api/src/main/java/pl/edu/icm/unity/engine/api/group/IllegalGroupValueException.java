/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.group;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Thrown when group value is invalid.
 * @author K. Benedyczak
 */
public class IllegalGroupValueException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalGroupValueException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalGroupValueException(String msg)
	{
		super(msg);
	}
}
