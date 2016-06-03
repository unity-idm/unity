/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.tx;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.tx.TransactionalExt;

/**
 * Hazelcast transactional runner - runs in a transaction using the Hazelcast infrastructure.
 * @author K. Benedyczak
 */
@Component(HzTransactionalRunner.NAME)
public class HzTransactionalRunner implements TransactionalRunner
{
	public static final String NAME = "HzTransactionalRunner";
	
	@TransactionalExt(storageEngine=StorageEngine.hz)
	@Override
	public void runInTransaction(TxRunnable code)
	{
		code.run();
	}

	@TransactionalExt(storageEngine=StorageEngine.hz)
	@Override
	public <T> T runInTransactionRet(TxRunnableRet<T> code)
	{
		return code.run();
	}
	
	@TransactionalExt(storageEngine=StorageEngine.hz, autoCommit = false)
	@Override
	public void runInTransactionNoAutoCommit(TxRunnable code)
	{
		code.run();
		
	}
	
	@TransactionalExt(storageEngine=StorageEngine.hz, autoCommit = false)
	@Override
	public <T> T runInTransactionNoAutoCommitRet(TxRunnableRet<T> code)
	{
		return code.run();
	}
}
