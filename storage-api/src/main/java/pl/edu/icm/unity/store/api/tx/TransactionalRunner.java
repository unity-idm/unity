/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.tx;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Implementations allow for running arbitrary code in transaction. Useful when Transactional annotation won't
 * work because of class which doesn't implement interface or is in unsupported package.
 * 
 * @author K. Benedyczak
 */
public interface TransactionalRunner
{
	void runInTransaction(TxRunnable code);
	
	<T> T runInTransactionRet(TxRunnableRet<T> code);
	
	void runInTransactionNoAutoCommit(TxRunnable code);
	
	<T> T runInTransactionNoAutoCommitRet(TxRunnableRet<T> code);

	void runInTransactionThrowing(TxRunnableThrowing code) throws EngineException;
	
	<T> T runInTransactionRetThrowing(TxRunnableThrowingRet<T> code) throws EngineException;
	
	public interface TxRunnable
	{
		void run();
	}

	public interface TxRunnableRet<T>
	{
		T run();
	}
	
	public interface TxRunnableThrowing
	{
		void run() throws EngineException;
	}

	public interface TxRunnableThrowingRet<T>
	{
		T run() throws EngineException;
	}
}
