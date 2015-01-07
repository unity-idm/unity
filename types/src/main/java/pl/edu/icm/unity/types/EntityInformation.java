/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.Date;

/**
 * Stores information about entity, besides its identities, credentials and basic information as id.
 * @author K. Benedyczak
 */
public class EntityInformation
{
	private EntityState state;
	private Date scheduledOperationTime;
	private EntityScheduledOperation scheduledOperation;
	
	public EntityInformation(EntityState state)
	{
		this.state = state;
	}

	public EntityState getState()
	{
		return state;
	}

	public void setState(EntityState state)
	{
		this.state = state;
	}

	public Date getScheduledOperationTime()
	{
		return scheduledOperationTime;
	}

	public void setScheduledOperationTime(Date scheduledOperationTime)
	{
		this.scheduledOperationTime = scheduledOperationTime;
	}

	public EntityScheduledOperation getScheduledOperation()
	{
		return scheduledOperation;
	}

	public void setScheduledOperation(EntityScheduledOperation scheduledOperation)
	{
		this.scheduledOperation = scheduledOperation;
	}
}
