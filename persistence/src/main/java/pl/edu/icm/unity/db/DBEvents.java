/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.EventSerializer;
import pl.edu.icm.unity.db.mapper.local.EventsMapper;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.EventBean;
import pl.edu.icm.unity.db.model.ResolvedEventBean;
import pl.edu.icm.unity.exceptions.IllegalArgumentException;
import pl.edu.icm.unity.server.events.Event;

/**
 * Persistence of events. Events are stored in the local DB.
 * @author K. Benedyczak
 */
@Component
public class DBEvents
{
	private LocalDBSessionManager db;
	private EventSerializer serializer;
	private DBLimits limits;
	
	@Autowired
	public DBEvents(LocalDBSessionManager dbSessionManager, EventSerializer serializer, DB db)
	{
		this.db = dbSessionManager;
		this.limits = db.getDBLimits();
		this.serializer = serializer;
	}
	
	public void addEvent(Event event, String listenerId)
	{
		String eventStr = serializer.toJson(event);
		EventBean bean = new EventBean(new Date(0), eventStr, listenerId);
		checkLimits(bean);
		SqlSession sql = db.getSqlSession(false);
		try
		{
			EventsMapper mapper = sql.getMapper(EventsMapper.class);
			mapper.insertEvent(bean);
		} finally
		{
			db.releaseSqlSession(sql);	
		}
	}

	public void removeEvent(long id)
	{
		SqlSession sql = db.getSqlSession(false);
		try
		{
			EventsMapper mapper = sql.getMapper(EventsMapper.class);
			mapper.deleteEvent(id);
		} finally
		{
			db.releaseSqlSession(sql);	
		}

	}
	
	public void updateExecution(long id, Date newExecution, int failures)
	{
		SqlSession sql = db.getSqlSession(false);
		try
		{
			EventBean param = new EventBean();
			param.setId(id);
			param.setNextProcessing(newExecution);
			param.setFailures(failures);
			EventsMapper mapper = sql.getMapper(EventsMapper.class);
			mapper.updateEvent(param);
		} finally
		{
			db.releaseSqlSession(sql);	
		}
	}

	public List<ResolvedEventBean> getEventsForProcessing(Date date)
	{
		List<EventBean> raw;
		SqlSession sql = db.getSqlSession(false);
		try
		{
			EventsMapper mapper = sql.getMapper(EventsMapper.class);
			raw = mapper.selectEventsForProcessing(date);
		} finally
		{
			db.releaseSqlSession(sql);	
		}
		
		List<ResolvedEventBean> ret = new ArrayList<ResolvedEventBean>(raw.size());
		for (EventBean rawB: raw)
		{
			Event event = serializer.fromJson(rawB.getEvent());
			ret.add(new ResolvedEventBean(rawB.getId(), rawB.getNextProcessing(), 
					event, rawB.getListenerId()));
		}
		return ret;
	}
	
	private void checkLimits(EventBean event)
	{
		if (event.getListenerId().length() > limits.getNameLimit())
			throw new IllegalArgumentException("Event listener id is too long, " +
					"max is " + limits.getNameLimit() + " name is " + event.getListenerId());
		if (event.getEvent().length() > limits.getContentsLimit())
			throw new IllegalArgumentException("Event contents is too long, " +
					"max is " + limits.getContentsLimit() + " size is " + event.getEvent().length());
	}
}
