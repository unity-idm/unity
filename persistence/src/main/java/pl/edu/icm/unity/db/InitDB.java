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

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Initializes DB if was not.
 * @author K. Benedyczak
 */
public class InitDB
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InitDB.class);
	
	private void performUpdate(DB db, String operationPfx)
	{
		Collection<String> ops = new TreeSet<String>(db.getMyBatisConfiguration().getMappedStatementNames());
		SqlSession session = db.getSqlSession(ExecutorType.BATCH, false);
		for (String name: ops)
			if (name.startsWith(operationPfx))
				session.update(name);
		session.commit();
		session.close();		
	}
	
	private void initDB(DB db)
	{
		log.info("Initializing DB");
		performUpdate(db, "initdb");
		SqlSession session = db.getSqlSession(false);
		try
		{
			session.insert("initVersion");
		} finally
		{
			session.close();
		}
	}

	private void initData(DB db) throws InternalException
	{
		log.info("Inserting initial data");
	}
	
	public void initIfNeeded(DB db) throws FileNotFoundException, IOException, InternalException
	{
		SqlSession session = db.getSqlSession(true);
		try
		{
			session.selectOne("getDBVersion");
			log.info("Database initialized, skipping creation");
		} catch (PersistenceException e)
		{
			initDB(db);
			initData(db);
		}
	}
}
