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
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atomikos.jdbc.AtomikosDataSourceBean;

import eu.unicore.util.db.DBPropertiesHelper;
import pl.edu.icm.unity.exceptions.InternalException;


/**
 * Provides initial MyBatis initialization and support for obtaining MyBatis SqlSessions.
 * 
 * @author K. Benedyczak
 */
@Component
public class DBSessionManager implements SessionManager
{
	public static final String DEF_MAPCONFIG_LOCATION = "pl/edu/icm/unity/store/rdbms/mapper/mapconfig.xml";

	public static final int SESSION_KEEP_WARN_TIME = 3000;
	
	private SqlSessionFactory sqlMapFactory;

	@Autowired
	public DBSessionManager(DBConfiguration config, AtomikosDataSourceBean dsBean) 
			throws InternalException, IOException
	{
		sqlMapFactory = loadMybatis(config, dsBean);
	}
	
	private SqlSessionFactory loadMybatis(DBConfiguration config, AtomikosDataSourceBean dsBean) throws IOException
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
		
		SqlSessionFactory fromXML = builder.build(reader, properties);
		
		TransactionFactory txFactory = new SpringManagedTransactionFactory();
		Environment env = new Environment("def", txFactory, dsBean);
		fromXML.getConfiguration().setEnvironment(env);
		return fromXML;
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
		return sqlMapFactory.openSession(executor, !transactional);
	}
}
