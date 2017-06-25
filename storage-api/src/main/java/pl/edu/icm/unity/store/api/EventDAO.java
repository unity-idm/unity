/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.base.event.EventExecution;

/**
 * {@link EventExecution} DAO, used to persist heavy weight events.
 * @author K. Benedyczak
 */
public interface EventDAO extends BasicCRUDDAO<EventExecution>
{
	String DAO_ID = "EventDAO";
	String NAME = "event execution";
	
	List<EventExecution> getEligibleForProcessing(Date date);
	void updateExecution(long id, Date newExecution, int failures);
}
