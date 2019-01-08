/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

import org.springframework.context.ApplicationEventPublisher;

/**
 * Unchecked engine exception, used mainly in scenarios where spring cannot
 * handle checked exceptions e.g. where {@link ApplicationEventPublisher} is
 * used to invoke synchronous event handling, and underlying method fails.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RuntimeEngineException extends RuntimeException
{
	public RuntimeEngineException(String message)
	{
		super(message);
	}
	
	public RuntimeEngineException(EngineException e)
	{
		super(e);
	}

	public RuntimeEngineException(String message, EngineException e)
	{
		super(message, e);
	}
}
