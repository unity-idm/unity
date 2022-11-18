/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.endpoint;

public class IllegalEndpointException extends IllegalStateException
{
	public IllegalEndpointException()
	{
		super();
	}

	public IllegalEndpointException(String s)
	{
		super(s);
	}

	public IllegalEndpointException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public IllegalEndpointException(Throwable cause)
	{
		super(cause);
	}
}
