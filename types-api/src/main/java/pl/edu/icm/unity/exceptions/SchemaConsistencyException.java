/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when operation would cause database schema consistency violation. Example is removal of
 * attribute class which is being assigned to a group or entity in a group.
 * @author K. Benedyczak
 */
public class SchemaConsistencyException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public SchemaConsistencyException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public SchemaConsistencyException(String msg)
	{
		super(msg);
	}
}
