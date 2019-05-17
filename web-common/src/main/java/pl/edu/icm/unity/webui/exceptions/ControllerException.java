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
		WARN, ERROR
	}

	private Type type;
	private String caption;
	
	public ControllerException(String errorCaption, Throwable cause)
	{
		this(Type.ERROR, errorCaption, "", cause);
	}

	public ControllerException(String errorCaption, String errorDetails, Throwable cause)
	{
		this(Type.ERROR, errorCaption, errorDetails, cause);
	}

	public ControllerException(Type type, String errorCaption, String errorDetails,
			Throwable cause)
	{
		super(errorDetails, cause);
		this.caption = errorCaption;
		this.type = type;
	}
	
	public static ControllerException warning(String errorCaption, Throwable cause)
	{
		return new ControllerException(Type.WARN, errorCaption, "", cause);
	}

	public Type getType()
	{
		return type;
	}

	public String getCaption()
	{
		return caption;
	}

	public String getDetails()
	{
		return getMessage();
	}
}
