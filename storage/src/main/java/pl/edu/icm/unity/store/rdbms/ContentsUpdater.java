/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;

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
	private static final String DATA_SCHEMA_MIGRATION_SUPPORTED_UP_TO_DB_VERSION = "2_3_0";
	
	public void update(long oldDbVersion) throws IOException, EngineException
	{
		assertMigrationsAreMatchingApp();
		migrateFromVersion2_2_0();
	}
	
	private void assertMigrationsAreMatchingApp() throws IOException
	{
		if (!DATA_SCHEMA_MIGRATION_SUPPORTED_UP_TO_DB_VERSION.equals(AppDataSchemaVersion.DB_VERSION))
		{
			throw new InternalException("The data migration code was not updated "
					+ "to the latest version of data schema. "
					+ "This should be fixed by developers.");
		}
	}
	
	private void migrateFromVersion2_2_0() throws IOException, EngineException
	{
		throw new IllegalStateException("Not implemented!");
	}
}




