/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.i18n;

public class MessageBundleSpec
{
	private String name;
	private String baseLocation;
	
	public MessageBundleSpec(String name, String baseLocation)
	{
		this.name = name;
		this.baseLocation = baseLocation;
	}

	public String getName()
	{
		return name;
	}

	public String getBaseLocation()
	{
		return baseLocation;
	}
}
