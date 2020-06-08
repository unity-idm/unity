/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;

/**
 * Thrown when there is a problem with processing Fido registration or authentication.
 * @author R. Ledzinski
 */
public class FidoException extends RuntimeException
{
	public FidoException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public FidoException(String msg)
	{
		super(msg);
	}

	//TODO implement getLocalizedMessage()
}
