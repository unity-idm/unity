/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.exceptions;

/**
 * Thrown to signal UI element about problem with engine operation.
 * 
 * @author P.Piernik
 *
 */
public class ControllerException extends Exception
{
	public enum Type
	{
		Warn, Error
	}

	private Type type;
	private String capion;
	
	public ControllerException(String errorCapion, Throwable cause)
	{
		this(Type.Error, errorCapion, "", cause);
	}

	public ControllerException(String errorCapion, String errorDetails, Throwable cause)
	{
		this(Type.Error, errorCapion, errorDetails, cause);
	}

	public ControllerException(Type type, String errorCapion, String errorDetails,
			Throwable cause)
	{
		super(errorDetails, cause);
		this.capion = errorCapion;
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}

	public String getCaption()
	{
		return capion;
	}

	public String getDetails()
	{
		return getMessage();
	}
}
