/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.bulkops.BulkProcessingInternal;
import pl.edu.icm.unity.engine.endpoint.InternalEndpointManagement;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.StorageCleaner;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Implementation of general maintenance.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class ServerManagementImpl implements ServerManagement
{
	private Logger log = Log.getLogger(Log.U_SERVER, ServerManagementImpl.class);
	private ImportExport dbDump;
	private StorageCleaner initDb;
	private EngineInitialization engineInit;
	private InternalAuthorizationManager authz;
	private UnityServerConfiguration config;
	private InternalEndpointManagement endpointMan;
	private TransactionalRunner tx;
	private BulkProcessingInternal bulkProcessing;
	
	
	@Autowired
	public ServerManagementImpl(TransactionalRunner tx, ImportExport dbDump, StorageCleaner initDb,
			EngineInitialization engineInit, InternalEndpointManagement endpointMan,
			InternalAuthorizationManager authz, ExecutorsService executorsService, 
			UnityServerConfiguration config,
			BulkProcessingInternal bulkProcessing)
	{
		this.tx = tx;
		this.dbDump = dbDump;
		this.initDb = initDb;
		this.engineInit = engineInit;
		this.endpointMan = endpointMan;
		this.authz = authz;
		this.config = config;
		this.bulkProcessing = bulkProcessing;
		executorsService.getService().scheduleWithFixedDelay(new ClenupDumpsTask(), 20, 60, TimeUnit.SECONDS);
	}


	@Override
	public void resetDatabase() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		bulkProcessing.removeAllRules();
		initDb.reset();
		endpointMan.undeployAll();
		engineInit.initializeDatabaseContents();
	}


	@Override
	@Transactional
	public File exportDb() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		try
		{
			File exportFile = createExportFile();
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(exportFile));
			dbDump.store(os);
			return exportFile;
		} catch (JsonGenerationException e)
		{
			throw new InternalException("Error creating JSON from database contents", e);
		} catch (IOException e)
		{
			throw new InternalException("Error writing database contents to disk", e);
		}
	}


	@Override
	public void importDb(File from) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		tx.runInTransaction(() -> {
			initDb.deleteEverything();
			try
			{
				BufferedInputStream is = new BufferedInputStream(new FileInputStream(from));
				dbDump.load(is);
			} catch (Exception e)
			{
				throw new InternalException("Database import failed. " +
						"Database should not be changed.", e);
			}
			initDb.runPostImportCleanup();
		});
		endpointMan.undeployAll();
		engineInit.initializeDatabaseContents();
	}
	
	private class ClenupDumpsTask implements Runnable
	{
		private static final long DUMP_STORE_TIME = 600000;
		
		@Override
		public void run()
		{
			File exportsDirectory = getExportDirectory();
			File[] files = exportsDirectory.listFiles();
			if (files == null)
			{
				log.error("Can not list exports directory " + exportsDirectory + "; cleanup failed");
				return;
			}
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
	
	private File getExportDirectory()
	{
		File workspace = config.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true);
		File exportDir = new File(workspace, ServerManagement.DB_DUMP_DIRECTORY);
		if (!exportDir.exists())
			exportDir.mkdir();
		return exportDir;
	}
	
	private String getExportFilePrefix()
	{
		return "export-";
	}
	
	private String getExportFileSuffix()
	{
		return ".json";
	}
	
	private File createExportFile() throws IOException
	{
		File exportDir = getExportDirectory();
		String[] list = exportDir.list();
		if (list == null)
			throw new IOException("Can not list database dumps directory, I/O error");
		if (list.length > 1)
			throw new IOException("Maximum number of database dumps was reached. " +
					"Subsequent dumps can be created in few minutes.");
		return File.createTempFile(getExportFilePrefix(), getExportFileSuffix(), exportDir);
	}
}
