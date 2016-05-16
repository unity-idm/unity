/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when type is invalid. The type is generic and context dependent - it can be 
 * for instance identity type or attribute value syntax type.
 * @author K. Benedyczak
 */
public class IllegalTypeException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalTypeException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalTypeException(String msg)
	{
		super(msg);
	}
}
