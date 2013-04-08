/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.mapper.local;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.db.model.EventBean;

/**
 * Events handling
 * @author K. Benedyczak
 */
public interface EventsMapper
{
	public void insertEvent(EventBean event);
	public void deleteEvent(long id);
	public void updateEvent(EventBean event);
	public List<EventBean> selectEventsForProcessing(Date date);
}
