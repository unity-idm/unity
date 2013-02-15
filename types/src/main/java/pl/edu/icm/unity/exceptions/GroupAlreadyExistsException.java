/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when a created group already exists in DB.
 * @author K. Benedyczak
 */
public class GroupAlreadyExistsException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public GroupAlreadyExistsException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public GroupAlreadyExistsException(String msg)
	{
		super(msg);
	}
}
