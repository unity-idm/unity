/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when there is merge conflict which shall be reported
 * @author K. Benedyczak
 */
public class MergeConflictException extends EngineException
{

	public MergeConflictException(String msg)
	{
		super(msg);
	}
}
