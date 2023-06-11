/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.store.api.EntityDAO;

/**
 * Implements support for scheduled operations on entities.
 * @author K. Benedyczak
 */
@Component
public class SheduledOperationHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, SheduledOperationHelper.class);
	private EntityDAO entityDAO;
	
	
	@Autowired
	public SheduledOperationHelper(EntityDAO entityDAO)
	{
		this.entityDAO = entityDAO;
	}

	/**
	 * If entity is in the state {@link EntityState#onlyLoginPermitted} this method clears the 
	 *  removal of the entity: state is set to enabled and user ordered removal is removed.
	 * @param entityId
	 * @param sqlMap
	 * @throws IllegalIdentityValueException
	 * @throws IllegalTypeException
	 */
	public void clearScheduledRemovalStatus(long entityId) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		EntityInformation info = entityDAO.getByKey(entityId);
		if (info.getState() != EntityState.onlyLoginPermitted)
			return;
		log.info("Removing scheduled removal of an account [as the user is being logged] for entity " + 
			entityId);
		info.setState(EntityState.valid);
		info.setRemovalByUserTime(null);
		entityDAO.updateByKey(entityId, info);
	}

	public void setScheduledOperationByAdmin(long entityId, Date when,
			EntityScheduledOperation operation) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		if (operation != null && when == null)
			throw new IllegalArgumentException("Date must be set for the scheduled operation");
		EntityInformation info = entityDAO.getByKey(entityId);

		if (operation == null)
		{
			info.setScheduledOperation(null);
			info.setScheduledOperationTime(null);
		} else
		{
			info.setScheduledOperation(operation);
			info.setScheduledOperationTime(when);
		}

		entityDAO.updateByKey(entityId, info);
	}

	public void setScheduledRemovalByUser(long entityId, Date when) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		EntityInformation info = entityDAO.getByKey(entityId);

		if (when == null)
		{
			info.setRemovalByUserTime(null);
		} else
		{
			info.setRemovalByUserTime(when);
			info.setState(EntityState.onlyLoginPermitted);
		}

		entityDAO.updateByKey(entityId, info);
	}
	
	
	/**
	 * Performs all scheduled operations due by now
	 * @param sqlMap
	 * @return the time when the earliest scheduled operation should take place. If there is no such operation 
	 * returned time is very far in future.
	 */
	public Date performScheduledEntityOperations()
	{
		List<EntityInformation> all = entityDAO.getAll();
		Date now = new Date();
		Date ret = new Date(Long.MAX_VALUE);
		for (EntityInformation entityInfo: all)
		{
			if (isSetAndAfter(now, entityInfo.getScheduledOperationTime()))
			{
				EntityScheduledOperation op = entityInfo.getScheduledOperation();
				performScheduledOperationInternal(op, entityInfo);
			} else if (isSetAndAfter(now, entityInfo.getRemovalByUserTime()))
			{
				performScheduledOperationInternal(EntityScheduledOperation.REMOVE, entityInfo);
			}
			
			Date nextOp = entityInfo.getScheduledOperationTime();
			if (nextOp != null && nextOp.before(ret))
				ret = nextOp;
		}
		return ret;
	}

	public void performScheduledOperation(long entityId, EntityScheduledOperation op)
	{
		EntityInformation info = entityDAO.getByKey(entityId);
		performScheduledOperationInternal(op, info);
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
