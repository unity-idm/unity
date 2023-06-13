/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.tx;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.store.tx.TransactionState;

public class SQLTransactionState implements TransactionState
{
	private SqlSession sql;
	private List<Runnable> actions = new ArrayList<>();
	
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
		runPostCommitActions();
	}

	@Override
	public List<Runnable> getPostCommitActions()
	{
		return actions;
	}
}