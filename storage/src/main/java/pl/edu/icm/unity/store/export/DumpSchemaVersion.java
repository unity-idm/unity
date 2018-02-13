/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.export;

/**
 * Enumerates schema versions with their metadata
 * @author K. Benedyczak
 */
public enum DumpSchemaVersion
{
	V_INITIAL2(3, "2.0.0", "1.9.x"),
	V_INITIAL2_4_x(4, "2.5.0", "2.4.x");
	
	private String name;
	private String previousName;
	private int versionCode; 
	
	DumpSchemaVersion(int versionCode, String name, String previousName)
	{
		this.versionCode = versionCode;
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

	public int getVersionCode()
	{
		return versionCode;
	}
}
