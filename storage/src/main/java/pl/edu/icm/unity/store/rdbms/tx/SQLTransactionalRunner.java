/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.tx;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.tx.TransactionalExt;

/**
 * RDBMS transactional runner - runs in a transaction using the RDBMS engine.
 * @author K. Benedyczak
 */
@Component(SQLTransactionalRunner.NAME)
public class SQLTransactionalRunner implements TransactionalRunner
{
	public static final String NAME = "SQLTransactionalRunner";
	
	@TransactionalExt(storageEngine=StorageEngine.rdbms)
	@Override
	public void runInTransaction(TxRunnable code)
	{
		code.run();
	}

	@TransactionalExt(storageEngine=StorageEngine.rdbms)
	@Override
	public <T> T runInTransactionRet(TxRunnableRet<T> code)
	{
		return code.run();
	}
	
	@TransactionalExt(storageEngine=StorageEngine.rdbms, autoCommit = false)
	@Override
	public void runInTransactionNoAutoCommit(TxRunnable code)
	{
		code.run();
	}
	
	@TransactionalExt(storageEngine=StorageEngine.rdbms, autoCommit = false)
	@Override
	public <T> T runInTransactionNoAutoCommitRet(TxRunnableRet<T> code)
	{
		return code.run();
	}

	@TransactionalExt(storageEngine=StorageEngine.rdbms)
	@Override
	public void runInTransactionThrowing(TxRunnableThrowing code) throws EngineException
	{
		code.run();
	}
	
	@TransactionalExt(storageEngine=StorageEngine.rdbms)
	@Override
	public <T> T runInTransactionRetThrowing(TxRunnableThrowingRet<T> code) throws EngineException
	{
		return code.run();
	}
}
