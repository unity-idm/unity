/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.AppDataSchemaVersion;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.migration.InDBSchemaUpdater;
import pl.edu.icm.unity.store.migration.from2_4.InDBUpdateFromSchema2_2;
import pl.edu.icm.unity.store.migration.from2_5.InDBUpdateFromSchema2_3;
import pl.edu.icm.unity.store.migration.from2_6.InDBUpdateFromSchema2_4;
import pl.edu.icm.unity.store.migration.from2_7.InDBUpdateFromSchema2_5;

/**
 * Updates DB contents. Note that this class is not updating DB schema (it is done in {@link InitDB}).
 *  
 * @author K. Benedyczak
 */
@Component
public class ContentsUpdater
{	/**
	 * To which version we can migrate. In principle this should be always equal to 
	 * {@link AppDataSchemaVersion#DB_VERSION} but is duplicated here as a defensive check: 
	 * when bumping it please make sure any required data migrations were implemented here.  
	 */
	private static final String DATA_SCHEMA_MIGRATION_SUPPORTED_UP_TO_DB_VERSION = "2_7_0";
	
	@Autowired
	private TransactionalRunner txManager;
	@Autowired
	private InDBUpdateFromSchema2_2 from2_4;
	@Autowired
	private InDBUpdateFromSchema2_3 from2_5;
	@Autowired
	private InDBUpdateFromSchema2_4 from2_6;
	@Autowired
	private InDBUpdateFromSchema2_5 from2_7;
	
	public void update(long oldDbVersion) throws IOException, EngineException
	{
		assertMigrationsAreMatchingApp();
		
		if (oldDbVersion < 20300)
			migrateFromSchemaVersion(from2_4);
		
		if (oldDbVersion < 20400)
			migrateFromSchemaVersion(from2_5);

		if (oldDbVersion < 20500)
			migrateFromSchemaVersion(from2_6);

		if (oldDbVersion < 20600)
			migrateFromSchemaVersion(from2_7);
	}
	
	private void assertMigrationsAreMatchingApp() throws IOException
	{
		if (!DATA_SCHEMA_MIGRATION_SUPPORTED_UP_TO_DB_VERSION.equals(AppDataSchemaVersion.CURRENT.getDbVersion()))
		{
			throw new InternalException("The data migration code was not updated "
					+ "to the latest version of data schema. "
					+ "This should be fixed by developers.");
		}
	}
	
	private void migrateFromSchemaVersion(InDBSchemaUpdater updater) throws IOException, EngineException
	{
		txManager.runInTransactionThrowing(() -> 
		{
			try
			{
				updater.update();
			} catch (IOException e)
			{
				throw new EngineException("Migration failed", e);
			}	
		});
	}
}




