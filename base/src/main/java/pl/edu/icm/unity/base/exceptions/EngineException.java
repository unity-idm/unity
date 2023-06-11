/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.exceptions;

/**
 * Super class of all API exceptions.
 * @author K. Benedyczak
 */
public class EngineException extends Exception
{
	public EngineException()
	{
	}
	
	public EngineException(String msg)
	{
		super(msg);
	}
	
	public EngineException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public EngineException(Throwable cause) 
	{
		super("", cause);
	}
}
