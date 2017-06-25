/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;

import pl.edu.icm.unity.base.event.EventExecution;
import pl.edu.icm.unity.store.api.EventDAO;
import pl.edu.icm.unity.store.hz.GenericBasicHzCRUD;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;


/**
 * Hazelcast implementation of event store.
 * @author K. Benedyczak
 */
@Repository(EventHzStore.STORE_ID)
public class EventHzStore extends GenericBasicHzCRUD<EventExecution> implements EventDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Autowired
	public EventHzStore(EventRDBMSStore rdbmsDAO)
	{
		super(STORE_ID, NAME, EventRDBMSStore.BEAN, rdbmsDAO);
	}

	@Override
	public List<EventExecution> getEligibleForProcessing(Date date)
	{
		TransactionalMap<Long, EventExecution> hMap = getMap();
		List<EventExecution> ret = new ArrayList<>();
		EntryObject e = new PredicateBuilder().getEntryObject();
		@SuppressWarnings("unchecked")
		Predicate<Long, EventExecution> predicate = e.get("nextProcessing").lessThan(date);
		ret.addAll(hMap.values(predicate));
		return ret;
	}

	@Override
	public void updateExecution(long id, Date newExecution, int failures)
	{
		TransactionalMap<Long, EventExecution> hMap = getMap();
		EventExecution old = hMap.get(id);
		if (old == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		old.setFailures(failures);
		old.setNextProcessing(newExecution);
		hMap.put(id, old);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"updateExecution", id, newExecution, failures));
	}
}
