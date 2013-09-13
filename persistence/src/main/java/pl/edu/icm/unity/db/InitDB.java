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
 * If so no change is committed. It is responsible for initialization of both the main and the local database.
 * @author K. Benedyczak
 */
@Component
public class InitDB
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InitDB.class);

	private DBSessionManager db;
	private LocalDBSessionManager localDb;

	@Autowired
	public InitDB(DBSessionManager db, LocalDBSessionManager localDb)
	{
		this.db = db;
		this.localDb = localDb;
	}

	/**
	 * Drops everything(!!) and recreates the initial DB state.
	 */
	public void resetDatabase()
	{
		log.info("Database will be totally wiped");
		performUpdate(db, "cleardb-");
		performUpdate(localDb, "cleardb-");
		log.info("The whole contents removed");
		initDB();
	}
	
	public void initIfNeeded() throws FileNotFoundException, IOException, InternalException
	{
		SqlSession session = db.getSqlSession(false);
		try
		{
			session.selectOne("getDBVersion");
			db.releaseSqlSession(session);
			log.info("Database initialized, skipping creation");
		} catch (PersistenceException e)
		{
			db.releaseSqlSession(session);
			initDB();
		}
	}
	
	/**
	 * Deletes all main DB records except version
	 * @param session
	 * @param resetIndexes if false indexes are not reset to the initial value. Useful for tests, 
	 * normally indexes should be reset.
	 */
	public void deleteEverything(SqlSession session, boolean resetIndexes)
	{
		Collection<String> ops = new TreeSet<String>(db.getMyBatisConfiguration().getMappedStatementNames());
		for (String name: ops)
			if (name.startsWith("deletedb-"))
				session.update(name);
		if (resetIndexes)
		{
			for (String name: ops)
				if (name.startsWith("resetIndex-"))
					session.update(name);
		}
	}
	
	private void performUpdate(SessionManager db, String operationPfx)
	{
		Collection<String> ops = new TreeSet<String>(db.getMyBatisConfiguration().getMappedStatementNames());
		SqlSession session = db.getSqlSession(ExecutorType.BATCH, true);
		try
		{
			for (String name: ops)
				if (name.startsWith(operationPfx))
					session.update(name);
			session.commit();
		} finally
		{
			db.releaseSqlSession(session);
		}
	}
	
	private void initDB()
	{
		log.info("Initializing DB schema");
		performUpdate(db, "initdb");
		performUpdate(localDb, "initdb");
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
