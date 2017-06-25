/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.rdbmsflush;

import java.util.List;

/**
 * Ordered list of {@link RDBMSMutationEvent}s to be applied in a single transaction.
 * 
 * @author K. Benedyczak
 */
public class RDBMSEventsBatch
{
	private List<RDBMSMutationEvent> events;

	protected RDBMSEventsBatch()
	{
	}
	
	public RDBMSEventsBatch(List<RDBMSMutationEvent> events)
	{
		this.events = events;
	}

	public List<RDBMSMutationEvent> getEvents()
	{
		return events;
	}
}
