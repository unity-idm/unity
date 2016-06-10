/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.events;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.event.EventExecution;
import pl.edu.icm.unity.store.api.EventDAO;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;


/**
 * RDBMS storage of {@link EventExecution}
 * @author K. Benedyczak
 */
@Repository(EventRDBMSStore.BEAN)
public class EventRDBMSStore extends GenericRDBMSCRUD<EventExecution, EventBean> 
					implements EventDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public EventRDBMSStore(EventExecutionJsonSerializer jsonSerializer)
	{
		super(EventsMapper.class, jsonSerializer, NAME);
	}

	@Override
	public long create(EventExecution obj)
	{
		long ret = super.create(obj);
		obj.setId(ret);
		return ret;
	}
	
	@Override
	public List<EventExecution> getEligibleForProcessing(Date date)
	{
		EventsMapper mapper = SQLTransactionTL.getSql().getMapper(EventsMapper.class);
		List<EventBean> rawEvents = mapper.selectEventsForProcessing(date);
		return convertList(rawEvents);
	}

	@Override
	public void updateExecution(long id, Date newExecution, int failures)
	{
		EventBean param = new EventBean();
		param.setId(id);
		param.setNextProcessing(newExecution);
		param.setFailures(failures);
		EventsMapper mapper = SQLTransactionTL.getSql().getMapper(EventsMapper.class);
		mapper.updateExecution(param);
	}
}
