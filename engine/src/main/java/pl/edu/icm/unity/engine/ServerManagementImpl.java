/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.InitDB;
import pl.edu.icm.unity.db.export.ImportExport;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.endpoints.InternalEndpointManagement;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

import com.fasterxml.jackson.core.JsonGenerationException;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * Implementation of general maintenance.
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class ServerManagementImpl implements ServerManagement
{
	private Logger log = Log.getLogger(Log.U_SERVER, ServerManagementImpl.class);
	private DBSessionManager db;
	private ImportExport dbDump;
	private InitDB initDb;
	private EngineInitialization engineInit;
	private AuthorizationManager authz;
	private UnityServerConfiguration config;
	private InternalEndpointManagement endpointMan;
	
	
	@Autowired
	public ServerManagementImpl(DBSessionManager db, ImportExport dbDump, InitDB initDb,
			EngineInitialization engineInit, InternalEndpointManagement endpointMan,
			AuthorizationManager authz, ExecutorsService executorsService, UnityServerConfiguration config)
	{
		this.db = db;
		this.dbDump = dbDump;
		this.initDb = initDb;
		this.engineInit = engineInit;
		this.endpointMan = endpointMan;
		this.authz = authz;
		this.config = config;
		executorsService.getService().scheduleWithFixedDelay(new ClenupDumpsTask(), 20, 60, TimeUnit.SECONDS);
	}


	@Override
	public void resetDatabase() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		initDb.resetDatabase();
		endpointMan.undeployAll();
		engineInit.initializeDatabaseContents();
	}


	@Override
	public File exportDb() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			File ret;
			try
			{
				ret = dbDump.exportDB(sql);
			} catch (JsonGenerationException e)
			{
				throw new InternalException("Error creating JSON from database contents", e);
			} catch (IOException e)
			{
				throw new InternalException("Error writing database contents to disk", e);
			}
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}


	@Override
	public void importDb(File from, boolean resetIndexes) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			initDb.deleteEverything(sql, resetIndexes);
			try
			{
				dbDump.importDB(from, sql);
			} catch (Exception e)
			{
				throw new InternalException("Database import failed. " +
						"Database should not be changed.", e);
			}
			initDb.runPostImportCleanup(sql);
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		endpointMan.undeployAll();
		engineInit.initializeDatabaseContents();
	}
	
	private class ClenupDumpsTask implements Runnable
	{
		private static final long DUMP_STORE_TIME = 600000;
		
		@Override
		public void run()
		{
			File exportsDirectory = dbDump.getExportDirectory();
			File[] files = exportsDirectory.listFiles();
			long now = System.currentTimeMillis();
			
			for (File file: files)
			{
				if (file.lastModified() + DUMP_STORE_TIME < now)
				{
					log.debug("Removing the old, temporary, database dump from the workspace: " 
							+ file);
					file.delete();
					continue;
				}
			}
		}
	}

	@Override
	public void reloadConfig() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		try
		{
			config.reloadIfChanged();
		} catch (ConfigurationException e)
		{
			throw new InternalException("Error in configuration file", e);
		} catch (IOException e)
		{
			throw new InternalException("Error reading configuration file", e);
		}
		
	}


	@Override
	public String loadConfigurationFile(String path) throws EngineException
	{       
		authz.checkAuthorization(AuthzCapability.maintenance);
		File f = new File(path);
		try
		{
			return FileUtils.readFileToString(f);
		} catch (IOException e)
		{
			throw new InternalException("Error loading configuration file " + path, e);
		}
	}
}
