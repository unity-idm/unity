/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.common;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Thrown to signal UI element about problem with engine operation. 
 * @author P.Piernik
 *
 */
public class ControllerException extends EngineException
{
	public enum Type
	{
		Warn, Error
	}

	private Type type;
	private String errorCapion;

	
	public ControllerException(String errorCapion, Throwable cause)
	{
		this(Type.Error, errorCapion, "", cause);
	}
	
	public ControllerException(String errorCapion , String errorDetails, Throwable cause)
	{
		this(Type.Error, errorCapion, errorDetails, cause);
	}

	public ControllerException(Type type, String errorCapion ,String errorDetails, Throwable cause)
	{
		super(errorDetails, cause);
		this.errorCapion = errorCapion;
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}

	public String getErrorCaption()
	{
		return errorCapion;
	}
	
	public String getErrorDetails()
	{
		return getMessage();
	}
}
