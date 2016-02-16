/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import pl.edu.icm.unity.exceptions.EngineException;


public interface TransactionalRunner
{
	public void runInTransaction(TxRunnable code) throws EngineException;
	
	public <T> T runInTransactionRet(TxRunnableRet<T> code) throws EngineException;
	
	public interface TxRunnable
	{
		void run() throws EngineException;
	}

	public interface TxRunnableRet<T>
	{
		T run() throws EngineException;
	}
}
