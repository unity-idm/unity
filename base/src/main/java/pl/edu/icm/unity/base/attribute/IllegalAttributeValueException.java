/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.attribute;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Thrown when attribtue value is invalid.
 * @author K. Benedyczak
 */
public class IllegalAttributeValueException extends EngineException
{
	public IllegalAttributeValueException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalAttributeValueException(String msg)
	{
		super(msg);
	}
}
