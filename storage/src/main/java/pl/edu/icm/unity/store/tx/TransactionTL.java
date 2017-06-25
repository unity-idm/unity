/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
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
	public static void manualCommit()
	{
		get().getCurrent().manualCommit();
	}

	private static TransactionsState<? extends TransactionState> get()
	{
		if (HzTransactionTL.getState().isEmpty())
		{
			return SQLTransactionTL.getState();
		} else
		{
			return HzTransactionTL.getState();
		}
	}
}
