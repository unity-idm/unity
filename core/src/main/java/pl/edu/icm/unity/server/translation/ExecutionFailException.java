/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Throw to signal that further execution of the translation profile should be stopped and that the whole 
 * process should be finished with error.
 * @author K. Benedyczak
 */
public class ExecutionFailException extends EngineException
{
	public ExecutionFailException(String message)
	{
		super(message);
	}
}
