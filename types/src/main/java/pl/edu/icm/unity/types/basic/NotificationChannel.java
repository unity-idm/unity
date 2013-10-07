/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import pl.edu.icm.unity.types.DescribedObjectImpl;

/**
 * Notification channel.
 * @author K. Benedyczak
 */
public class NotificationChannel extends DescribedObjectImpl
{
	private String configuration;
	private String facilityId;
	
	public NotificationChannel(String name, String description, String configuration, String facilityId)
	{
		super(name, description);
		this.configuration = configuration;
		this.facilityId = facilityId;
	}

	public NotificationChannel()
	{
	}

	public String getConfiguration()
	{
		return configuration;
	}

	public void setConfiguration(String configuration)
	{
		this.configuration = configuration;
	}

	public String getFacilityId()
	{
		return facilityId;
	}

	public void setFacilityId(String facilityId)
	{
		this.facilityId = facilityId;
	}
}
