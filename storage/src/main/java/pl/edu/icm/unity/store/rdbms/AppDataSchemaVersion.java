/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

/**
 * Those constants control current data version and versions supported for migration
 * 
 * @author K. Benedyczak
 */
public class AppDataSchemaVersion
{
	/**
	 * The current version implemented by this version software
	 */
	public static final String DB_VERSION = "2_3_0"; //DB version 2_3_0 corresponds to the 2.5.0 Unity release
	
	/**
	 * The oldest version of software which can be automatically updated to the current version 
	 */
	public static final String OLDEST_SUPPORTED_DB_VERSION = "2_2_0";	//DB version 2_2_0 corresponds to the 2.0.0 Unity release
}
