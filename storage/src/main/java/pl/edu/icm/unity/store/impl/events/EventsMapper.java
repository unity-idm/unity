/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.events;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;

/**
 * Events handling
 * @author K. Benedyczak
 */
public interface EventsMapper extends BasicCRUDMapper<EventBean>
{
	void updateExecution(EventBean event);
	List<EventBean> selectEventsForProcessing(Date date);
}
