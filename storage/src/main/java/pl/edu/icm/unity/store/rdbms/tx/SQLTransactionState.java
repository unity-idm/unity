/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.tx;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.store.tx.TransactionState;

public class SQLTransactionState implements TransactionState
{
	private SqlSession sql;
	
	public SQLTransactionState(SqlSession sql)
	{
		this.sql = sql;
	}
	
	public SqlSession getSql()
	{
		return sql;
	}

	@Override
	public void manualCommit()
	{
		sql.commit();
	}
}