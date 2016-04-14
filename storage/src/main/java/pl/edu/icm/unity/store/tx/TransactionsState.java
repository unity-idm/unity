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
		if (transactionsStack.isEmpty())
			throw new IllegalStateException("There is no transaction in the context. This is a bug.");
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
}
