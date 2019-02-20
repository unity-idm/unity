/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

/**
 * Enumerates information about data schema versions: JSON dump version revision, DB version revision 
 * and corresponding public information about released version.
 * 
 * @author K. Benedyczak
 */
public enum DumpSchemaVersion
{
	V_INITIAL_2_0_0(3, "2_2_0", "2.0.0", "1.9.x"),
	V_SINCE_2_5_0(4, "2_3_0", "2.5.0", "2.0.0"),
	V_SINCE_2_6_0(5, "2_4_0", "2.6.0", "2.5.0"),
	V_SINCE_2_7_0(6, "2_5_0", "2.7.0", "2.6.0"),
	V_SINCE_2_8_0(7, "2_6_0", "2.8.0", "2.7.0"),
	V_SINCE_2_9_0(8, "2_7_0", "2.9.0", "2.8.0");
	
	private String name;
	private String previousName;
	private int jsonDumpVersionCode;
	private String dbVersion; 
	
	DumpSchemaVersion(int jsonDumpVersion, String dbVersion, String name, String previousName)
	{
		this.jsonDumpVersionCode = jsonDumpVersion;
		this.dbVersion = dbVersion;
		this.name = name;
		this.previousName = previousName;
	}

	public String getName()
	{
		return name;
	}

	public String getPreviousName()
	{
		return previousName;
	}

	public int getJsonDumpVersion()
	{
		return jsonDumpVersionCode;
	}

	public String getDbVersion()
	{
		return dbVersion;
	}
}
