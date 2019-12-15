/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import pl.edu.icm.unity.store.export.DumpSchemaVersion;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;
import pl.edu.icm.unity.store.rdbms.ContentsUpdater;
import pl.edu.icm.unity.store.rdbms.InitDB;

/**
 * Those constants control current data version and versions supported for migration
 * <p>
 * <h2>MIGRATION CREATION INSTRUCTION</h2>
 * <ol>
 * <li> Change CURRENT value here to newly created DumpSchemaVersion
 * <li> Change DB schema version in Initdb-common.xml
 * <li> in migration.xml add appropriate SQL schema migration. In minimal situation it must update VERSION flag in DB.
 * Bump {@link InitDB} SQL_SCHEMA_MIGRATION_SUPPORTED_UP_TO_DB_VERSION.
 * <li> Create in-place migration implementing InDBSchemaUpdater
 * (suggested separate package ...unity.store.migration.fromX_Y)
 * <li> Create JSON dump migration implementing {@link JsonDumpUpdate} in the above created package
 * <li> Bump {@link ContentsUpdater} DATA_SCHEMA_MIGRATION_SUPPORTED_UP_TO_DB_VERSION
 * <li> Wire up DB migration in {@link ContentsUpdater}
 * </ol>
 * @author K. Benedyczak
 */
public class AppDataSchemaVersion
{
	public static final DumpSchemaVersion CURRENT = DumpSchemaVersion.V_SINCE_3_2_0;
	
	/**
	 * The oldest version of software which can be automatically updated to the current version 
	 */
	public static final String OLDEST_SUPPORTED_DB_VERSION = DumpSchemaVersion.V_INITIAL_2_0_0.getDbVersion();
}
