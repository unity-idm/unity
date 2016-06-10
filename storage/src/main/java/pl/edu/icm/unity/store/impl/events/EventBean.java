/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.events;

import java.util.Date;

import pl.edu.icm.unity.store.rdbms.BaseBean;

/**
 * Event stored in DB.
 * @author K. Benedyczak
 */
public class EventBean extends BaseBean
{
	private Date nextProcessing; 
	private String listenerId;
	private int failures;
	
	public EventBean()
	{
	}

	public EventBean(Date nextProcessing, String listenerId, int failures, byte[] contents)
	{
		super(null, contents);
		this.nextProcessing = nextProcessing;
		this.listenerId = listenerId;
		this.failures = failures;
	}

	public String getListenerId()
	{
		return listenerId;
	}

	public void setListenerId(String listenerId)
	{
		this.listenerId = listenerId;
	}

	public int getFailures()
	{
		return failures;
	}

	public void setFailures(int failures)
	{
		this.failures = failures;
	}

	public Date getNextProcessing()
	{
		return nextProcessing;
	}

	public void setNextProcessing(Date nextProcessing)
	{
		this.nextProcessing = nextProcessing;
	}
}
