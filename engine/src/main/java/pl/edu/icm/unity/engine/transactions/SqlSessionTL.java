/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.transactions;

import org.apache.ibatis.session.SqlSession;

/**
 * Thread local SQL session, set by AOP.
 * @author K. Benedyczak
 */
public class SqlSessionTL
{
	public static ThreadLocal<SqlSession> sqlSession = new ThreadLocal<>();
	
	static ThreadLocal<TransactionsState> transactionState = new TransactionsThreadLocal();
	
	public static SqlSession get()
	{
		return sqlSession.get();
	}
	
	private static class TransactionsThreadLocal extends ThreadLocal<TransactionsState>
	{
		public TransactionsState initialValue()
		{
			return new TransactionsState();
		}
	}
}
