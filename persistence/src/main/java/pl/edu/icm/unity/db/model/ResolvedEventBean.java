/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

import java.util.Date;

import pl.edu.icm.unity.server.events.Event;

/**
 * Resolved event for returning into engine
 * @author K. Benedyczak
 */
public class ResolvedEventBean extends EventBeanBase
{
	private Event event;

	public ResolvedEventBean()
	{
	}

	public ResolvedEventBean(long id, Date nextProcessing, Event event, String listenerId)
	{
		super(nextProcessing, listenerId);
		this.event = event;
		setId(id);
	}

	public Event getEvent()
	{
		return event;
	}
	public void setEvent(Event event)
	{
		this.event = event;
	}

	@Override
	public String toString()
	{
		return "[event=" + event + " " + super.toString()
				+ "]";
	}
}
