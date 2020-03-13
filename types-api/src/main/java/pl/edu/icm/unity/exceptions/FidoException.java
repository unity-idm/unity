/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when there is problem with processing Fido registration or authentication.
 * @author R. Ledzinski
 */
public class FidoException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public FidoException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public FidoException(String msg)
	{
		super(msg);
	}
}
