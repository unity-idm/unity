/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 17, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.store.rdbms;

import java.io.FileReader;
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
 * Provides initial MyBatis initialization and support for obtaining MyBatis SqlSessions.
 * 
 * @author K. Benedyczak
 */
@Component
public class DBSessionManager
{
	public static final String DEF_MAPCONFIG_LOCATION = "pl/edu/icm/unity/store/rdbms/mapper/mapconfig.xml";

	public static final int SESSION_KEEP_WARN_TIME = 3000;
	
	private SqlSessionFactory sqlMapFactory;

	@Autowired
	public DBSessionManager(DBConfiguration config) 
			throws InternalException, IOException
	{
		sqlMapFactory = loadMybatis(config);
	}
	
	private SqlSessionFactory loadMybatis(DBConfiguration config) throws IOException
	{
		String mapFile = config.getFileValueAsString(DBConfiguration.DBCONFIG_FILE, false);
		SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
		Reader reader;
		if (mapFile != null)
			reader = new FileReader(mapFile);
		else
			reader = Resources.getResourceAsReader(DEF_MAPCONFIG_LOCATION);
		Properties properties = new Properties();
		properties.setProperty(DBPropertiesHelper.DIALECT,
				config.getValue(DBPropertiesHelper.DIALECT));
		properties.setProperty(DBPropertiesHelper.DRIVER,
				config.getValue(DBPropertiesHelper.DRIVER));
		properties.setProperty(DBPropertiesHelper.PASSWORD,
				config.getValue(DBPropertiesHelper.PASSWORD));
		properties.setProperty(DBPropertiesHelper.URL,
				config.getValue(DBPropertiesHelper.URL));
		properties.setProperty(DBPropertiesHelper.USER,
				config.getValue(DBPropertiesHelper.USER));
		
		return builder.build(reader, properties);
	}

	public Configuration getMyBatisConfiguration()
	{
		return sqlMapFactory.getConfiguration();
	}
	
	public SqlSession getSqlSession(boolean transactional)
	{
		return getSqlSession(ExecutorType.SIMPLE, transactional);
	}

	public SqlSession getSqlSession(ExecutorType executor, boolean transactional)
	{
		return sqlMapFactory.openSession(executor, !transactional);
	}
}
