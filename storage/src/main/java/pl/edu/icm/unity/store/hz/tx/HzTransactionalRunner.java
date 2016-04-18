/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.tx;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.internal.StorageEngine;
import pl.edu.icm.unity.base.internal.Transactional;
import pl.edu.icm.unity.base.internal.TransactionalRunner;

/**
 * Hazelcast transactional runner - runs in a transaction using the Hazelcast infrastructure.
 * @author K. Benedyczak
 */
@Component(HzTransactionalRunner.NAME)
public class HzTransactionalRunner implements TransactionalRunner
{
	public static final String NAME = "HzTransactionalRunner";
	
	@Transactional(storageEngine=StorageEngine.hz)
	@Override
	public void runInTransaction(TxRunnable code)
	{
		code.run();
	}

	@Transactional(storageEngine=StorageEngine.hz)
	@Override
	public <T> T runInTransactionRet(TxRunnableRet<T> code)
	{
		return code.run();
	}
}
