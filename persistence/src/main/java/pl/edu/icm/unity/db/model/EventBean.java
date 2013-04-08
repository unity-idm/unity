/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

import java.util.Date;

/**
 * Event stored in DB.
 * @author K. Benedyczak
 */
public class EventBean extends EventBeanBase
{
	private String event;

	public EventBean()
	{
	}

	public EventBean(Date nextProcessing, String event, String listenerId)
	{
		super(nextProcessing, listenerId);
		this.event = event;
	}

	public String getEvent()
	{
		return event;
	}
	public void setEvent(String event)
	{
		this.event = event;
	}
}
