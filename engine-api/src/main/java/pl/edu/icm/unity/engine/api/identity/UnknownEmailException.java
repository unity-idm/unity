/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.identity;

import pl.edu.icm.unity.engine.api.exceptions.RuntimeEngineException;

/**
 * Thrown when email value is unknown
 * 
 * @author P.Piernik z
 */
public class UnknownEmailException extends RuntimeEngineException
{
	public UnknownEmailException(String msg)
	{
		super(msg);
	}
}
