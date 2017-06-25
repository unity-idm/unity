/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

/**
 * Thrown when a retries of an transaction fails. Wrapped exception is the exception of the last try.
 * @author K. Benedyczak
 */
public class TxPersistenceException extends Exception
{
	public TxPersistenceException(Exception cause)
	{
		super(cause);
	}
}
