/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;

/**
 * Provides serialization of Exception to JSON format. Only part of the information is shown.
 * Uses default JOSN serialization.
 * 
 * @author K. Benedyczak
 */
public class JsonError
{
	private String message;
	private String error;
	
	public JsonError(String message, String error)
	{
		this.message = message;
		this.error = error;
	}

	public JsonError(Exception ex)
	{
		this(ex.getMessage(), ex.getClass().getSimpleName());
	}

	protected JsonError()
	{
	}
	
	public String getMessage()
	{
		return message;
	}

	public String getError()
	{
		return error;
	}
	
	@Override
	public String toString()
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Shouldn't happen: can't serialize error to JSON string", e);
		}
	}
}
