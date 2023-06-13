/*
 * Copyright (c) 2007, 2008 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Mar 17, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.store.rdbms;

import static pl.edu.icm.unity.store.AppDataSchemaVersion.CURRENT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.AppDataSchemaVersion;
import pl.edu.icm.unity.store.impl.groups.GroupBean;
import pl.edu.icm.unity.store.impl.groups.GroupIE;
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
	private int dbVersionAtServerStarup;
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
			dbVersionAtServerStarup = CURRENT.getAppSchemaVersion();
			return;
		}
		
		if (dbVersion == null)
		{
			throw new InternalException("The database seems to be corrupted "
					+ "(the schema version table is empty). Most probably the only possible "
					+ "way to fix this is to drop it and create a new, empty one.");
		}

		dbVersionAtServerStarup = parseDBVersion(dbVersion);
		int dbVersionOfSoftware = CURRENT.getAppSchemaVersion();
		assertMigrationsAreMatchingApp();
		if (dbVersionAtServerStarup > dbVersionOfSoftware)
		{
			throw new InternalException("The database schema version " + dbVersion + 
					" is newer then supported by this version of the server. "
					+ "Please upgrade the server software.");
		} else if (dbVersionAtServerStarup < dbVersionOfSoftware)
		{
			if (dbVersionAtServerStarup < parseDBVersion(AppDataSchemaVersion.OLDEST_SUPPORTED_DB_VERSION))
				throw new InternalException("The database schema version " + dbVersion + 
						" is older then the last supported version. "
						+ "Please make sure you are updating Unity from the previous version"
						+ " and check release notes.");
			updateSchema(dbVersionAtServerStarup);
		}
	}
	
	private void assertMigrationsAreMatchingApp()
	{
		int maxMigration = db.getMyBatisConfiguration().getMappedStatementNames().stream()
			.filter(name -> name.startsWith(UPDATE_SCHEMA_PFX))
			.map(name -> name.substring(UPDATE_SCHEMA_PFX.length()).split("-")[0])
			.map(Integer::parseInt)
			.max(Integer::compareTo).get();
		if (maxMigration != CURRENT.getAppSchemaVersion())
		{
			throw new InternalException("The SQL migration code was not updated "
					+ "to the latest version of data schema. "
					+ "This should be fixed by developers.");
		}
	}
	
	/**
	 * Deletes all main DB records except version. After deletion creates the root group.
	 */
	public void deleteEverything(SqlSession session)
	{
		log.info("Database contents will be completely deleted");
		Collection<String> ops = new TreeSet<>(db.getMyBatisConfiguration().getMappedStatementNames());
		for (String name: ops)
			if (name.startsWith("deletedb-"))
				session.update(name);
		for (String name: ops)
			if (name.startsWith("resetIndex-"))
				session.update(name);
		log.info("Database contents was completely deleted");
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
		Collection<String> ops = new TreeSet<>(db.getMyBatisConfiguration().getMappedStatementNames());
		SqlSession session = db.getSqlSession(ExecutorType.BATCH, true);
		try
		{
			for (String name: ops)
				if (name.startsWith(operationPfx))
				{
					session.update(name);
					log.trace("Update run: {}", name);
					if (name.endsWith("-requireCommit"))
					{
						session.commit();
						log.debug("per-update commit performed");
					}
				}
			session.commit();
		} finally
		{
			session.close();
			log.debug("Finished update with prefix {}", operationPfx);
		}
	}
	
	private void initDB()
	{
		log.info("Initializing DB schema");
		performUpdate(db, "initdb");
		SqlSession session = db.getSqlSession(false);
		try
		{
			session.insert("initVersion", Integer.toString(CURRENT.getAppSchemaVersion()));
			createRootGroup(session);
		} finally
		{
			session.close();
			log.info("Initialized DB schema");
		}
	}
	
	private void createRootGroup(SqlSession session)
	{
		GroupsMapper groups = session.getMapper(GroupsMapper.class);
		GroupBean root = new GroupBean("/", null);
		root.setContents(GroupJsonSerializer.createRootGroupContents());
		groups.createRoot(root);
	}
	
	public static int parseDBVersion(String version)
	{
		return version.contains("_") ? parseLegacyDBVersion(version) : Integer.parseInt(version);
	}

	private static int parseLegacyDBVersion(String version)
	{
		String[] components = version.split("_");
		return Integer.parseInt(components[1]);
	}

	
	private void updateSchema(int initialDBVersion)
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
				int updaterVersion = Integer.parseInt(version[0]);
				if (updaterVersion > initialDBVersion)
				{
					log.info("Run update db schema script " + name);
					session.update(name);
				}
			}
			session.commit();
		} finally
		{
			session.close();
		}
		log.info("Updated DB schema to the actual version " + CURRENT.getAppSchemaVersion());
	}
	
	public void updateContents() throws IOException, EngineException
	{
		if (dbVersionAtServerStarup < CURRENT.getAppSchemaVersion())
		{
			log.info("Updating DB contents to the actual version");
			contentsUpdater.update(dbVersionAtServerStarup);
			log.info("Updated DB contents to the actual version {}", CURRENT.getAppSchemaVersion());
		}
	}

	public void deletePreImport(SqlSession session, List<String> objectTypes)
	{
		Collection<String> ops = new TreeSet<>(db.getMyBatisConfiguration().getMappedStatementNames());
	
		log.info("Following database elements will be cleared: " + objectTypes);
		List<String> copts = ops.stream().filter(n -> n.startsWith("deletedb-common")).collect(Collectors.toList());
		for (String o : copts)
		{
			session.update(o);
		}
		
		for (String eName : objectTypes)
		{
			List<String> sopts = ops.stream().filter(n -> n.startsWith("deletedb-" + eName))
					.collect(Collectors.toList());
			if (sopts.size() > 0)
			{
				for (String o : sopts)
				{
					session.update(o);
				}
			}else
			{
				session.update("deletedbvar", eName);
			}
		}
	
		log.info("Following database elements was cleared: " + objectTypes);
		if (objectTypes.contains(GroupIE.GROUPS_OBJECT_TYPE))
		{
			createRootGroup(session);
		}
	}
}
