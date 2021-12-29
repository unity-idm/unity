/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.events;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.event.EventExecution;
import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

/**
 * Serializes {@link EventExecution} to/from DB form.
 * @author K. Benedyczak
 */
@Component
class EventExecutionJsonSerializer implements RDBMSObjectSerializer<EventExecution, EventBean>
{
	@Override
	public EventBean toDB(EventExecution object)
	{
		byte[] serializedEvent = JsonUtil.serialize2Bytes(object.getEvent().toJson());
		EventBean bean = new EventBean(object.getNextProcessing(), object.getListenerId(),
				object.getFailures(), serializedEvent);
		bean.setId(object.getId());
		return bean;
	}

	@Override
	public EventExecution fromDB(EventBean bean)
	{
		PersistableEvent deserialized = new PersistableEvent(JsonUtil.parse(bean.getContents()));
		EventExecution ret = new EventExecution(deserialized, bean.getNextProcessing(), 
				bean.getListenerId(), bean.getFailures());
		ret.setId(bean.getId());
		return ret;
	}
	
	EventExecution fromJson(ObjectNode src)
	{
		try
		{
			return Constants.MAPPER.treeToValue(src, EventExecution.class);
		} catch (JsonProcessingException e)
		{
			throw new IllegalArgumentException("Invalid event execution JSON", e);
		}
	}
}
