/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;

/**
 * Thrown when there is not entity for given username or id.
 * @author R. Ledzinski
 */
public class NoEntityException extends FidoException
{
	public NoEntityException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public NoEntityException(String msg)
	{
		super(msg);
	}
}
