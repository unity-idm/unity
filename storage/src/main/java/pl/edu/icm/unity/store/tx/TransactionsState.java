/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Stores transaction state, useful when transactions are nested
 * @author K. Benedyczak
 */
public class TransactionsState<T extends TransactionState>
{
	private Deque<T> transactionsStack = new ArrayDeque<>();
	
	public void push(T transactionInfo)
	{
		transactionsStack.push(transactionInfo);
	}
	
	public T pop()
	{
		return transactionsStack.pop();
	}

	public T getCurrent()
	{
		if (transactionsStack.isEmpty())
			throw new IllegalStateException("There is no transaction in the context. This is a bug.");
		return transactionsStack.peek();
	}

	/**
	 * Gets the first transaction - transaction that is not subtransaction of any other.
	 * @return The first transaction
	 */
	T getRootTransaction()
	{
		if (transactionsStack.isEmpty())
			throw new IllegalStateException("There is no transaction in the context. This is a bug.");
		return transactionsStack.getLast();
	}
	
	public boolean isEmpty()
	{
		return transactionsStack.isEmpty();
	}

	public int size()
	{
		return transactionsStack.size();
	}
	
	public void clear()
	{
		transactionsStack.clear();
	}
	
	public boolean isSubtransaction()
	{
		return transactionsStack.size() > 1;
	}
	
	public static class TransactionsThreadLocal<T extends TransactionState> 
			extends ThreadLocal<TransactionsState<T>>
	{
		public TransactionsState<T> initialValue()
		{
			return new TransactionsState<>();
		}
	}
}
