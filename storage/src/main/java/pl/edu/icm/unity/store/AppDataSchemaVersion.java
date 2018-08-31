/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import pl.edu.icm.unity.store.export.DumpSchemaVersion;

/**
 * Those constants control current data version and versions supported for migration
 * 
 * @author K. Benedyczak
 */
public class AppDataSchemaVersion
{
	public static final DumpSchemaVersion CURRENT = DumpSchemaVersion.V_SINCE_2_7_0;
	
	/**
	 * The oldest version of software which can be automatically updated to the current version 
	 */
	public static final String OLDEST_SUPPORTED_DB_VERSION = DumpSchemaVersion.V_INITIAL_2_0_0.getDbVersion();
}
