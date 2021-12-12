/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * Thread local transaction state. Allows to manually manipulate transaction. 
 * 
 * <p>
 * Implementation note: the implementation is obviously quite ugly...
 * @author K. Benedyczak
 */
public class TransactionTL
{
	/**
	 * Manually commit current transaction.
	 */
	public static void manualCommit()
	{
		get().getCurrent().manualCommit();
	}

	/**
	 * Adds actions that should be executed after successful commit.
	 * @param action
	 * 		Action to be executed.
	 */
	static void addPostCommitAction(Runnable action)
	{
		get().getRootTransaction().addPostCommitAction(action);
	}

	private static TransactionsState<? extends TransactionState> get()
	{
		return SQLTransactionTL.getState();
	}
}
