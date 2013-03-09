/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 17, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Initializes DB schema and inserts the initial data. It is checked if DB was already initialized.
 * If so no change is commited.
 * @author K. Benedyczak
 */
@Component
public class InitDB
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InitDB.class);

	private DBSessionManager db;

	@Autowired
	public InitDB(DBSessionManager db)
	{
		super();
		this.db = db;
	}

	/**
	 * Drops everything(!!) and recreates the initial DB state.
	 */
	public void resetDatabase()
	{
		log.info("Database will be totally wiped");
		performUpdate("cleardb-");
		log.info("The whole contents removed");
		initDB();
	}
	
	public void initIfNeeded() throws FileNotFoundException, IOException, InternalException
	{
		SqlSession session = db.getSqlSession(false);
		try
		{
			session.selectOne("getDBVersion");
			log.info("Database initialized, skipping creation");
		} catch (PersistenceException e)
		{
			initDB();
		}
	}
	
	private void performUpdate(String operationPfx)
	{
		Collection<String> ops = new TreeSet<String>(db.getMyBatisConfiguration().getMappedStatementNames());
		SqlSession session = db.getSqlSession(ExecutorType.BATCH, true);
		for (String name: ops)
			if (name.startsWith(operationPfx))
				session.update(name);
		session.commit();
		db.releaseSqlSession(session);		
	}
	
	private void initDB()
	{
		log.info("Initializing DB schema");
		performUpdate("initdb");
		SqlSession session = db.getSqlSession(false);
		try
		{
			session.insert("initVersion");
			GroupsMapper groups = session.getMapper(GroupsMapper.class);
			GroupBean root = new GroupBean();
			root.setName(GroupResolver.ROOT_GROUP_NAME);
			groups.insertGroup(root);
		} finally
		{
			db.releaseSqlSession(session);		
		}
	}
}
