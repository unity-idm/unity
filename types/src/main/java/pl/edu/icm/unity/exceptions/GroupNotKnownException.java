/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when a requested group was not found in DB.
 * @author K. Benedyczak
 */
public class GroupNotKnownException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public GroupNotKnownException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public GroupNotKnownException(String msg)
	{
		super(msg);
	}
}
