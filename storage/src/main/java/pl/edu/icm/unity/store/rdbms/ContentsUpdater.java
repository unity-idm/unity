/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.AppDataSchemaVersion;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;

/**
 * Updates DB contents. Note that this class is not updating DB schema (it is done in {@link InitDB}).
 */
@Component
public class ContentsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, ContentsUpdater.class);
	private final TransactionalRunner txManager;
	private final List<InDBContentsUpdater> updaters;
	
	@Autowired
	public ContentsUpdater(List<InDBContentsUpdater> updaters, TransactionalRunner txManager)
	{
		this.updaters = updaters;
		this.txManager = txManager;
		this.updaters.sort((a, b) -> Integer.compare(a.getUpdatedVersion(), b.getUpdatedVersion()));
		int version = updaters.get(0).getUpdatedVersion();
		for (InDBContentsUpdater update: updaters)
		{
			if (update.getUpdatedVersion() != version)
				throw new IllegalStateException(
						"DB content updaters chain is inconsistent: no updater from version " + version);
			version++;
		}
		if (version != AppDataSchemaVersion.CURRENT.getAppSchemaVersion())
			throw new IllegalStateException(
					"DB content updaters chain is incomplete: no updater to current app version");
	}
	
	public void update(int oldDbVersion) throws IOException, EngineException
	{
		for (InDBContentsUpdater update: updaters)
		{
			int updateFrom = update.getUpdatedVersion();
			if (oldDbVersion <= updateFrom)
				migrateFromSchemaVersion(update);
				
		}
	}
	
	private void migrateFromSchemaVersion(InDBContentsUpdater updater) throws IOException, EngineException
	{
		txManager.runInTransactionThrowing(() -> 
		{
			try
			{
				log.info("Updating DB contents from version {}", updater.getUpdatedVersion());
				updater.update();
			} catch (IOException e)
			{
				throw new EngineException("Migration failed", e);
			}	
		});
	}
}




