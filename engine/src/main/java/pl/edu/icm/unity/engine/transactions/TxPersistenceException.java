/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.transactions;

import org.apache.ibatis.exceptions.PersistenceException;

/**
 * Thrown when a retries of an transaction fails. Wrapped exception is the exception of the last try.
 * @author K. Benedyczak
 */
public class TxPersistenceException extends PersistenceException
{
	public TxPersistenceException(PersistenceException cause)
	{
		super(cause);
	}
}
