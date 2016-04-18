/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.tx;

import com.hazelcast.transaction.TransactionContext;

import pl.edu.icm.unity.store.rdbmsflush.RDBMSEventsBatch;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.store.tx.TransactionsState;

/**
 * Thread local Hazelcast transaction state, set by AOP. Allows to obtain {@link TransactionContext} for accessing 
 * Hazelcast. Also RDBMS mutations are recorded in the thread local variable, until the transaction is committed.
 * Internally this is only an easy to use proxy to {@link TransactionsState}.
 * 
 * @author K. Benedyczak
 */
public class HzTransactionTL
{
	static ThreadLocal<TransactionsState<HzTransactionState>> transactionState = 
			new TransactionsState.TransactionsThreadLocal<>();
	
	public static TransactionContext getHzContext()
	{
		return transactionState.get().getCurrent().getHzContext();
	}

	public static void enqueueRDBMSMutation(RDBMSMutationEvent event)
	{
		transactionState.get().getCurrent().enqueueEvent(event);
	}

	public static RDBMSEventsBatch getCurrentRDBMSBatch()
	{
		return transactionState.get().getCurrent().getBatch();
	}
}
