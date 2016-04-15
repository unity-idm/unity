/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import org.apache.ibatis.session.SqlSession;

import com.hazelcast.transaction.TransactionContext;

import pl.edu.icm.unity.store.RDBMSEventsBatch;
import pl.edu.icm.unity.store.RDBMSMutationEvent;

/**
 * Thread local transaction state, set by AOP. Allows to obtain {@link SqlSession} for accessing MyBatis
 * and {@link TransactionContext} for accessing Hazelcast.
 * @author K. Benedyczak
 */
public class TransactionTL
{
	static ThreadLocal<TransactionsState> transactionState = new TransactionsThreadLocal();
	
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

	private static class TransactionsThreadLocal extends ThreadLocal<TransactionsState>
	{
		public TransactionsState initialValue()
		{
			return new TransactionsState();
		}
	}
}
