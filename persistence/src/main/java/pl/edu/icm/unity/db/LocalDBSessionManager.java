/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 17, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.db;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.db.DBPropertiesHelper;

import pl.edu.icm.unity.exceptions.InternalException;


/**
 * Provides initial MyBatis initialization and support for obtaining MyBatis SqlSessions
 * for the local (not replicated) H2 db.
 * 
 * @author K. Benedyczak
 */
@Component
public class LocalDBSessionManager implements SessionManager
{
	public static final String DEF_MAPCONFIG_LOCATION = "pl/edu/icm/unity/db/mapper-local/mapconfig.xml";

	private SqlSessionFactory sqlMapFactory;

	@Autowired
	public LocalDBSessionManager(DBConfiguration config) throws InternalException, IOException
	{
		sqlMapFactory = loadMybatis(config);
	}
	
	private SqlSessionFactory loadMybatis(DBConfiguration config) throws IOException
	{
		SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
		Reader reader = Resources.getResourceAsReader(DEF_MAPCONFIG_LOCATION);
		Properties properties = new Properties();
		properties.setProperty(DBPropertiesHelper.URL,
				config.getValue(DBConfiguration.LOCAL_DB_URL));
		
		return builder.build(reader, properties);
	}

	@Override
	public Configuration getMyBatisConfiguration()
	{
		return sqlMapFactory.getConfiguration();
	}
	
	@Override
	public SqlSession getSqlSession(boolean transactional)
	{
		return getSqlSession(ExecutorType.SIMPLE, transactional);
	}

	@Override
	public SqlSession getSqlSession(ExecutorType executor, boolean transactional)
	{
		SqlSession newSession = sqlMapFactory.openSession(executor, !transactional);
		return newSession;
	}

	@Override
	public void releaseSqlSession(SqlSession session)
	{
		session.close();
	}
}
