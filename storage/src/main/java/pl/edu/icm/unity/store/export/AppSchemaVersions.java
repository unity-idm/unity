/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

/**
 * Enumerates information about data schema versions. Mostly for convenience to record historical info 
 * about which revision matches which official version.
 * <p>
 * Note that before 3.2.0 release a much more complex scheme of versioning was used.
 * 
 * @author K. Benedyczak
 */
public enum AppSchemaVersions
{
	V_INITIAL_2_0_0(3, "2.0.0"),
	V_SINCE_2_5_0(4, "2.5.0"),
	V_SINCE_2_6_0(5, "2.6.0"),
	V_SINCE_2_7_0(6, "2.7.0"),
	V_SINCE_2_8_0(7, "2.8.0"),
	V_SINCE_3_0_0(8, "3.0.0"),
	V_SINCE_3_1_0(9, "3.1.0"),
	V_SINCE_3_2_0(10, "3.2.0"),
	V_SINCE_3_3_0(11, "3.3.0"),
	V_SINCE_3_4_0(12, "3.4.0"),
	V_SINCE_3_6_0(13, "3.6.0");
	
	private String name;
	private int appSchemaVersion;
	
	AppSchemaVersions(int appSchemaVersion, String name)
	{
		this.appSchemaVersion = appSchemaVersion;
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public int getAppSchemaVersion()
	{
		return appSchemaVersion;
	}
}
