/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.files;

/**
 * Thrown when a low level problem with read uri occurs. Unchecked.
 *
 * @author P.Piernik
 *
 */
public class URIAccessException extends RuntimeException
{
	public URIAccessException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public URIAccessException(String msg)
	{
		super(msg);
	}

}
