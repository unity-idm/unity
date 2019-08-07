/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.event;

import java.util.Date;

/**
 * {@link PersistableEvent} with information required for its execution using a fixed listener.
 * @author K. Benedyczak
 */
public class EventExecution
{
	private Long id;
	private Date nextProcessing; 
	private String listenerId;
	private int failures;
	private PersistableEvent event;

	protected EventExecution()
	{
	}

	public EventExecution(PersistableEvent event, Date nextProcessing, String listenerId, int failures)
	{
		this.nextProcessing = nextProcessing;
		this.listenerId = listenerId;
		this.event = event;
		this.failures = failures;
	}

	public PersistableEvent getEvent()
	{
		return event;
	}
	public void setEvent(PersistableEvent event)
	{
		this.event = event;
	}

	public String getListenerId()
	{
		return listenerId;
	}
	public void setListenerId(String listenerId)
	{
		this.listenerId = listenerId;
	}
	public Long getId()
	{
		return id;
	}
	public void setId(Long dbId)
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
		return "EventBeanBase [event=" + event + " id=" + id + ", nextProcessing=" + nextProcessing
				+ ", listenerId=" + listenerId + ", failures=" + failures + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((event == null) ? 0 : event.hashCode());
		result = prime * result + failures;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((listenerId == null) ? 0 : listenerId.hashCode());
		result = prime * result
				+ ((nextProcessing == null) ? 0 : nextProcessing.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventExecution other = (EventExecution) obj;
		if (event == null)
		{
			if (other.event != null)
				return false;
		} else if (!event.equals(other.event))
			return false;
		if (failures != other.failures)
			return false;
		if (id != other.id)
			return false;
		if (listenerId == null)
		{
			if (other.listenerId != null)
				return false;
		} else if (!listenerId.equals(other.listenerId))
			return false;
		if (nextProcessing == null)
		{
			if (other.nextProcessing != null)
				return false;
		} else if (!nextProcessing.equals(other.nextProcessing))
			return false;
		return true;
	}
}
