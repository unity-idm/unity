/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 17, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.store.rdbms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.AppDataSchemaVersion;
import pl.edu.icm.unity.store.impl.groups.GroupBean;
import pl.edu.icm.unity.store.impl.groups.GroupJsonSerializer;
import pl.edu.icm.unity.store.impl.groups.GroupsMapper;

/**
 * Initializes DB schema and inserts the initial data. It is checked if DB was already initialized.
 * If so no change is committed.
 * @author K. Benedyczak
 */
@Component
public class InitDB
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InitDB.class);
	private final String UPDATE_SCHEMA_PFX = "updateSchema-";
	
	/**
	 * To which version we can migrate. In principle this should be always equal to 
	 * {@link AppDataSchemaVersion#DB_VERSION} but is duplicated here as a defensive check: 
	 * when bumping it please make sure any required SQL schema updates were implemented.  
	 */
	private static final String SQL_SCHEMA_MIGRATION_SUPPORTED_UP_TO_DB_VERSION = "2_7_0";

	
	private long dbVersionAtServerStarup;
	private DBSessionManager db;
	private ContentsUpdater contentsUpdater;

	@Autowired
	public InitDB(DBSessionManager db, ContentsUpdater contentsUpdater) 
			throws FileNotFoundException, InternalException, IOException, EngineException
	{
		this.db = db;
		this.contentsUpdater = contentsUpdater;
	}

	/**
	 * Drops everything(!!) and recreates the initial DB state.
	 */
	public void resetDatabase()
	{
		log.info("Database will be totally wiped");
		performUpdate(db, "cleardb-");
		log.info("The whole contents removed");
		initDB();
	}
	
	public void initIfNeeded() throws FileNotFoundException, IOException, InternalException, EngineException
	{
		String dbVersion;
		SqlSession session = db.getSqlSession(false);
		try
		{
			dbVersion = session.selectOne("getDBVersion");
			session.close();
			log.info("Database initialized, skipping creation");
		} catch (PersistenceException e)
		{
			session.close();
			initDB();
			dbVersionAtServerStarup = dbVersion2Long(AppDataSchemaVersion.CURRENT.getDbVersion());
			return;
		}
		
		if (dbVersion == null)
		{
			throw new InternalException("The database seems to be corrupted "
					+ "(the schema version table is empty). Most probably the only possible "
					+ "way to fix this is to drop it and create a new, empty one.");
		}

		dbVersionAtServerStarup = dbVersion2Long(dbVersion);
		long dbVersionOfSoftware = dbVersion2Long(AppDataSchemaVersion.CURRENT.getDbVersion());
		assertMigrationsAreMatchingApp();
		if (dbVersionAtServerStarup > dbVersionOfSoftware)
		{
			throw new InternalException("The database schema version " + dbVersion + 
					" is newer then supported by this version of the server. "
					+ "Please upgrade the server software.");
		} else if (dbVersionAtServerStarup < dbVersionOfSoftware)
		{
			if (dbVersionAtServerStarup < dbVersion2Long(AppDataSchemaVersion.OLDEST_SUPPORTED_DB_VERSION))
				throw new InternalException("The database schema version " + dbVersion + 
						" is older then the last supported version. "
						+ "Please make sure you are updating Unity from the previous version"
						+ " and check release notes.");
			updateSchema(dbVersionAtServerStarup);
		}
	}
	
	private void assertMigrationsAreMatchingApp()
	{
		if (!SQL_SCHEMA_MIGRATION_SUPPORTED_UP_TO_DB_VERSION.equals(AppDataSchemaVersion.CURRENT.getDbVersion()))
		{
			throw new InternalException("The SQL migration code was not updated "
					+ "to the latest version of data schema. "
					+ "This should be fixed by developers.");
		}
	}
	
	/**
	 * Deletes all main DB records except version. After deletion creates the root group.
	 * @param session
	 */
	public void deleteEverything(SqlSession session)
	{
		Collection<String> ops = new TreeSet<>(db.getMyBatisConfiguration().getMappedStatementNames());
		for (String name: ops)
			if (name.startsWith("deletedb-"))
				session.update(name);
		for (String name: ops)
			if (name.startsWith("resetIndex-"))
				session.update(name);
		createRootGroup(session);
	}

	/**
	 * Runs DB-specific operations, which are needed after import. 
	 * @param session
	 */
	public void runPostImportCleanup(SqlSession session)
	{
		Collection<String> ops = new TreeSet<String>(db.getMyBatisConfiguration().getMappedStatementNames());
		for (String name: ops)
			if (name.startsWith("postDBImport-"))
				session.update(name);
	}
	
	private void performUpdate(DBSessionManager db, String operationPfx)
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
			session.close();
		}
	}
	
	private void initDB()
	{
		log.info("Initializing DB schema");
		performUpdate(db, "initdb");
		SqlSession session = db.getSqlSession(false);
		try
		{
			session.insert("initVersion");
			createRootGroup(session);
		} finally
		{
			session.close();
		}
	}
	
	private void createRootGroup(SqlSession session)
	{
		GroupsMapper groups = session.getMapper(GroupsMapper.class);
		GroupBean root = new GroupBean("/", null);
		root.setContents(JsonUtil.serialize2Bytes(
				GroupJsonSerializer.createRootGroupContents()));
		groups.createRoot(root);
	}
	
	public static long dbVersion2Long(String version)
	{
		String[] components = version.split("_");
		return Integer.parseInt(components[0])*10000 + Integer.parseInt(components[1])*100 + 
				Integer.parseInt(components[2]);
	}
	
	private void updateSchema(long currentVersion)
	{
		log.info("Updating DB schema to the actual version");
		Collection<String> ops = new TreeSet<String>(db.getMyBatisConfiguration().getMappedStatementNames());
		SqlSession session = db.getSqlSession(ExecutorType.BATCH, true);
		try
		{
			for (String name: ops)
			{
				if (!name.startsWith(UPDATE_SCHEMA_PFX))
					continue;
				
				String[] version = name.substring(UPDATE_SCHEMA_PFX.length()).split("-");
				Long schemaVersion = Long.parseLong(version[0]);
				if (schemaVersion > currentVersion)
					session.update(name);
			}
			session.commit();
		} finally
		{
			session.close();
		}
		log.info("Updated DB schema to the actual version " + AppDataSchemaVersion.CURRENT.getDbVersion());
	}

	
	public void updateContents() throws IOException, EngineException
	{
		long dbVersionOfSoftware = dbVersion2Long(AppDataSchemaVersion.CURRENT.getDbVersion());
		if (dbVersionAtServerStarup < dbVersionOfSoftware)
		{
			log.info("Updating DB contents to the actual version");
			contentsUpdater.update(dbVersionAtServerStarup);
			log.info("Updated DB contents to the actual version " + AppDataSchemaVersion.CURRENT.getDbVersion());
		}
	}
}
