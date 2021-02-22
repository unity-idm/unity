/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.events.EventProcessor;
import pl.edu.icm.unity.engine.events.EventProducingAspect;
import pl.edu.icm.unity.engine.events.InvocationEventContents;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;

/**
 * Applies scheduled operations on entities: removes them or disables.
 * @author K. Benedyczak
 */
@Component
public class EntitiesScheduledUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EntitiesScheduledUpdater.class);
	private UnityServerConfiguration config;
	private EntityDAO entityDAO;
	private EventProcessor eventProcessor;
	
	@Autowired
	public EntitiesScheduledUpdater(UnityServerConfiguration config, EntityDAO entityDAO, EventProcessor eventProcessor)
	{
		this.config = config;
		this.entityDAO = entityDAO;
		this.eventProcessor = eventProcessor;
	}
	
	@Transactional
	public synchronized Date updateEntities()
	{
		log.debug("Performing scheduled operations on entities");
		Date ret = performScheduledEntityOperations();
		
		long maxAsyncWait = config.getIntValue(UnityServerConfiguration.UPDATE_INTERVAL) * 1000;
		Date maxWait = new Date(System.currentTimeMillis() + maxAsyncWait);
		Date finalRet = ret.after(maxWait) ? maxWait : ret;
		log.debug("Scheduled operations on entities executed, next round scheduled at " + finalRet);
		return finalRet;
	}
	
	
	/**
	 * Performs all scheduled operations due by now
	 * @return the time when the earliest scheduled operation should take place. If there is no such operation 
	 * returned time is very far in future.
	 */
	private Date performScheduledEntityOperations()
	{
		Date now = new Date();
		Date ret = new Date(Long.MAX_VALUE);
		for (EntityInformation entityInfo: entityDAO.getAll())
		{
			if (isSetAndAfter(now, entityInfo.getScheduledOperationTime()))
			{
				EntityScheduledOperation op = entityInfo.getScheduledOperation();
				performScheduledOperationAndProduceEvent(op, entityInfo);
			} else if (isSetAndAfter(now, entityInfo.getRemovalByUserTime()))
			{
				performScheduledOperationAndProduceEvent(EntityScheduledOperation.REMOVE, entityInfo);
			}
			
			Date nextOp = entityInfo.getScheduledOperationTime();
			if (nextOp != null && nextOp.before(ret))
				ret = nextOp;
		}
		return ret;
	}
	
	private void performScheduledOperationAndProduceEvent(EntityScheduledOperation op,
			EntityInformation entityInfo)
	{
		try
		{
			performScheduledOperationInternal(op, entityInfo);
			produceEvent("performScheduledOperationInternal", null, op, entityInfo);

		} catch (Exception e)
		{

			produceEvent("performScheduledOperationInternal", e.toString(), op, entityInfo);
			throw e;
		}
	}
	
	private void performScheduledOperationInternal(EntityScheduledOperation op, EntityInformation entityInfo)
	{
		switch (op)
		{
		case DISABLE:
			log.info("Performing scheduled disable of entity " + entityInfo.getId());
			disableInternal(entityInfo);
			break;
		case REMOVE:
			log.info("Performing scheduled removal of entity " + entityInfo.getId());
			entityDAO.deleteByKey(entityInfo.getId());
			break;
		}	
	}
	
	private void produceEvent(String methodName, String e, EntityScheduledOperation op, EntityInformation entityInfo)
	{
		PersistableEvent event = new PersistableEvent(EventProducingAspect.CATEGORY_INVOCATION + "." + methodName,
				null, new Date());
		InvocationEventContents desc = new InvocationEventContents(methodName, 
				null, new Object[] {op, entityInfo}, e);
		event.setContents(desc.toJson());
		eventProcessor.fireEvent(event);	
	}
	
	private void disableInternal(EntityInformation entityInfo)
	{
		entityInfo.setState(EntityState.disabled);
		entityInfo.setScheduledOperation(null);
		entityInfo.setScheduledOperationTime(null);
		entityInfo.setRemovalByUserTime(null);
		entityDAO.updateByKey(entityInfo.getId(), entityInfo);
	}
	
	private boolean isSetAndAfter(Date now, Date date)
	{
		return date != null && !now.before(date);
	}
}
