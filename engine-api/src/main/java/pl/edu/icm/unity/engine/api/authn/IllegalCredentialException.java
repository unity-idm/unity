/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Thrown when credential definition is invalid.
 * @author K. Benedyczak
 */
public class IllegalCredentialException extends EngineException
{
	public IllegalCredentialException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalCredentialException(String msg)
	{
		super(msg);
	}
}
