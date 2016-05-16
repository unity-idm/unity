/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when a low level problem occurs, which in principle shouldn't happen. Example
 * can be database problem. Unchecked.
 * @author K. Benedyczak
 */
public class InternalException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public InternalException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public InternalException(String msg)
	{
		super(msg);
	}
}
