/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import pl.edu.icm.unity.store.export.AppSchemaVersions;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;

/**
 * Those constants control current data version and versions supported for migration
 * <p>
 * <h2>MIGRATION CREATION INSTRUCTION</h2>
 * <ol>
 * <li> Change CURRENT value here to newly created DumpSchemaVersion
 * <li> in migration.xml add appropriate SQL schema migration. In minimal situation it must update VERSION in DB.
 * <li> Create in-place migration implementing InDBSchemaUpdater
 * (suggested separate package ...unity.store.migration.fromX_Y)
 * <li> Create JSON dump migration implementing {@link JsonDumpUpdate} in the above created package
 * </ol>
 * @author K. Benedyczak
 */
public class AppDataSchemaVersion
{
	public static final AppSchemaVersions CURRENT = AppSchemaVersions.V_SINCE_3_8_0;
	
	/**
	 * The oldest version of software which can be automatically updated to the current version 
	 */
	public static final String OLDEST_SUPPORTED_DB_VERSION = "2_2_0";
}
