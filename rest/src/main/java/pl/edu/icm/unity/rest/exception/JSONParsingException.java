/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.exception;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Used to express a problem with invalid JSON input.
 * @author K. Benedyczak
 */
public class JSONParsingException extends JsonProcessingException
{
	public JSONParsingException(String msg, Throwable rootCause)
	{
		super(msg, rootCause);
	}

	public JSONParsingException(String msg)
	{
		super(msg);
	}
}
