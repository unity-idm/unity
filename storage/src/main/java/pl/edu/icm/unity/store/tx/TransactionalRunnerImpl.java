/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.store.api.tx.Transactional;

/**
 * Trivial transactional runner - useful for executing a code in a transaction, whenever it is too much overhead to
 * separate it into an interfaced method of a separate component.
 * @author K. Benedyczak
 */
@Component
public class TransactionalRunnerImpl implements TransactionalRunner
{
	@Override
	@Transactional
	public void runInTransaction(TxRunnable code)
	{
		code.run();
	}

	@Override
	@Transactional
	public <T> T runInTransactionRet(TxRunnableRet<T> code)
	{
		return code.run();
	}
}
