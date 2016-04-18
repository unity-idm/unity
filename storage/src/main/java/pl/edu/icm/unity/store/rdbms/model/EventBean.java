/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.model;

import java.util.Date;

/**
 * Event stored in DB.
 * @author K. Benedyczak
 */
public class EventBean extends EventBeanBase
{
	private byte[] event;

	public EventBean()
	{
	}

	public EventBean(Date nextProcessing, byte[] event, String listenerId)
	{
		super(nextProcessing, listenerId);
		this.event = event;
	}

	public byte[] getEvent()
	{
		return event;
	}
	public void setEvent(byte[] event)
	{
		this.event = event;
	}
}
