/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

import java.util.Date;

/**
 * For subclassing, see {@link EventBean} and {@link ResolvedEventBean}
 * @author K. Benedyczak
 */
public class EventBeanBase
{
	private long id;
	private Date nextProcessing; 
	private String listenerId;
	private int failures;

	public EventBeanBase()
	{
	}

	public EventBeanBase(Date nextProcessing, String listenerId)
	{
		this.nextProcessing = nextProcessing;
		this.listenerId = listenerId;
		failures = 0;
	}

	public String getListenerId()
	{
		return listenerId;
	}
	public void setListenerId(String listenerId)
	{
		this.listenerId = listenerId;
	}
	public long getId()
	{
		return id;
	}
	public void setId(long dbId)
	{
		this.id = dbId;
	}

	public Date getNextProcessing()
	{
		return nextProcessing;
	}

	public void setNextProcessing(Date nextProcessing)
	{
		this.nextProcessing = nextProcessing;
	}

	public int getFailures()
	{
		return failures;
	}

	public void setFailures(int failures)
	{
		this.failures = failures;
	}

	@Override
	public String toString()
	{
		return "EventBeanBase [id=" + id + ", nextProcessing=" + nextProcessing
				+ ", listenerId=" + listenerId + ", failures=" + failures + "]";
	}
}
