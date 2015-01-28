/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.i18n;

public class MessageBundleSpec
{
	private String name;
	private String baseLocation;
	private String[] enabledLocales;
	
	public MessageBundleSpec(String name, String baseLocation, String... enabledLocales)
	{
		this.name = name;
		this.baseLocation = baseLocation;
		this.enabledLocales = enabledLocales;
	}

	public String getName()
	{
		return name;
	}

	public String getBaseLocation()
	{
		return baseLocation;
	}

	public String[] getEnabledLocales()
	{
		return enabledLocales;
	}
}
