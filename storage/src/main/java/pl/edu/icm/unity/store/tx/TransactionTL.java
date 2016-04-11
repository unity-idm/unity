/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import org.apache.ibatis.session.SqlSession;

import com.hazelcast.transaction.HazelcastXAResource;
import com.hazelcast.transaction.TransactionContext;

/**
 * Thread local transaction state, set by AOP. Allows to obtain {@link SqlSession} for accessing MyBatis
 * and {@link TransactionContext} for accessing Hazelcast.
 * @author K. Benedyczak
 */
public class TransactionTL
{
	static ThreadLocal<TransactionsState> transactionState = new TransactionsThreadLocal();
	
	public static SqlSession getSql()
	{
		return transactionState.get().getCurrent().getSqlSession();
	}

	public static TransactionContext getHzContext()
	{
		return transactionState.get().getCurrent().getHzXAResource().getTransactionContext();
	}

	public static HazelcastXAResource getHzXAResource()
	{
		return transactionState.get().getCurrent().getHzXAResource();
	}
	
	private static class TransactionsThreadLocal extends ThreadLocal<TransactionsState>
	{
		public TransactionsState initialValue()
		{
			return new TransactionsState();
		}
	}
}
