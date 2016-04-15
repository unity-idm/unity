/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import org.apache.ibatis.session.SqlSession;

/**
 * Thread local RDBMS transaction state.
 * @author K. Benedyczak
 */
public class SQLTransactionTL
{
	static ThreadLocal<SqlSession> transactionState = new ThreadLocal<>();
	
	public static SqlSession getSql()
	{
		return transactionState.get();
	}

	public static void setCurrent(SqlSession sql)
	{
		transactionState.set(sql);
	}
}
