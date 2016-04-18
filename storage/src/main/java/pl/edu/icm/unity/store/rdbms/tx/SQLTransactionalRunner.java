/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.tx;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.internal.StorageEngine;
import pl.edu.icm.unity.base.internal.Transactional;
import pl.edu.icm.unity.base.internal.TransactionalRunner;

/**
 * RDBMS transactional runner - runs in a transaction using the RDBMS engine.
 * @author K. Benedyczak
 */
@Component(SQLTransactionalRunner.NAME)
public class SQLTransactionalRunner implements TransactionalRunner
{
	public static final String NAME = "SQLTransactionalRunner";
	
	@Transactional(storageEngine=StorageEngine.rdbms)
	@Override
	public void runInTransaction(TxRunnable code)
	{
		code.run();
	}

	@Transactional(storageEngine=StorageEngine.rdbms)
	@Override
	public <T> T runInTransactionRet(TxRunnableRet<T> code)
	{
		return code.run();
	}
}
