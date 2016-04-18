/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.tx;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.store.tx.TransactionsState;

/**
 * Thread local RDBMS transaction state. Allows to get the current {@link SqlSession}.
 * @author K. Benedyczak
 */
public class SQLTransactionTL
{
	static ThreadLocal<TransactionsState<SQLTransactionState>> transactionState = 
			new TransactionsState.TransactionsThreadLocal<>();
	
	public static SqlSession getSql()
	{
		return transactionState.get().getCurrent().getSql();
	}
}
