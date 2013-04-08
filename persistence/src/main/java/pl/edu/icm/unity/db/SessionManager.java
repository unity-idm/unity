/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;

/**
 * Common interface of session managers
 * @author K. Benedyczak
 */
public interface SessionManager
{
	public Configuration getMyBatisConfiguration();
	public SqlSession getSqlSession(boolean transactional);
	public SqlSession getSqlSession(ExecutorType executor, boolean transactional);
	public void releaseSqlSession(SqlSession session);
}
