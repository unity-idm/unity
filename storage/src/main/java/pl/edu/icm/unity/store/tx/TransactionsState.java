/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.ibatis.session.SqlSession;

import com.hazelcast.transaction.HazelcastXAResource;

import pl.edu.icm.unity.store.api.tx.Propagation;

/**
 * Stores transaction state, useful when transactions are nested
 * @author K. Benedyczak
 */
public class TransactionsState
{
	private Deque<TransactionState> transactionsStack = new ArrayDeque<>();
	
	public void push(TransactionState transactionInfo)
	{
		transactionsStack.push(transactionInfo);
	}
	
	public TransactionState pop()
	{
		return transactionsStack.pop();
	}

	public TransactionState getCurrent()
	{
		return transactionsStack.peek();
	}
	
	public boolean isEmpty()
	{
		return transactionsStack.isEmpty();
	}

	public boolean isSubtransaction()
	{
		return transactionsStack.size() > 1;
	}
	
	public static class TransactionState
	{
		private Propagation propagation;
		private SqlSession sqlSession;
		private HazelcastXAResource hzXAResource;
		
		public TransactionState(Propagation propagation, SqlSession sqlSession, HazelcastXAResource hzXAResource)
		{
			this.propagation = propagation;
			this.sqlSession = sqlSession;
			this.hzXAResource = hzXAResource;
		}
		public Propagation getPropagation()
		{
			return propagation;
		}
		public SqlSession getSqlSession()
		{
			return sqlSession;
		}
		public HazelcastXAResource getHzXAResource()
		{
			return hzXAResource;
		}
	}
}
