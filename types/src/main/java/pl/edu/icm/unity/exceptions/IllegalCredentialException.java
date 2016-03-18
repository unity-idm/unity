/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

import java.util.List;

import pl.edu.icm.unity.types.I18nStringSource;

/**
 * Thrown when credential definition is invalid.
 * @author K. Benedyczak
 */
public class IllegalCredentialException extends EngineException
{
	private List<I18nStringSource> details;

	public IllegalCredentialException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalCredentialException(String msg)
	{
		super(msg);
	}
	
	public IllegalCredentialException(String msg, List<I18nStringSource> details)
	{
		super(msg);
		this.details = details;
	}

	public List<I18nStringSource> getDetails()
	{
		return details;
	}
}
