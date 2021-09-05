/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when email value is unknown
 * 
 * @author P.Piernik z
 */
public class UnknownEmailException extends EngineException
{
	public UnknownEmailException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public UnknownEmailException(String msg)
	{
		super(msg);
	}
}
