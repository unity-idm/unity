/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

class TestTxRunner implements TransactionalRunner
{
	@Override
	public <T> T runInTransactionRet(TxRunnableRet<T> code)
	{
		return code.run();
	}
	
	@Override
	public void runInTransaction(TxRunnable code)
	{
		code.run();
	}

	@Override
	public void runInTransactionNoAutoCommit(TxRunnable code)
	{
		code.run();
	}

	@Override
	public <T> T runInTransactionNoAutoCommitRet(TxRunnableRet<T> code)
	{
		return code.run();
	}

	@Override
	public void runInTransactionThrowing(TxRunnableThrowing code) throws EngineException
	{
		code.run();
	}

	@Override
	public <T> T runInTransactionRetThrowing(TxRunnableThrowingRet<T> code)
			throws EngineException
	{
		return code.run();
	}
}
