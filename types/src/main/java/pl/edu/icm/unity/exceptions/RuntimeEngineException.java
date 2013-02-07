/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Super class of all API exceptions which need not to be caught.
 * @author K. Benedyczak
 */
public class RuntimeEngineException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public RuntimeEngineException(String msg)
	{
		super(msg);
	}
	
	public RuntimeEngineException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
