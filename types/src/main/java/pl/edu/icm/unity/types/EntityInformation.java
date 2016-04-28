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
	private EntityState entityState;
	private Date scheduledOperationTime;
	private EntityScheduledOperation scheduledOperation;
	private Date removalByUserTime;
	
	public EntityInformation(EntityState state)
	{
		this.entityState = state;
	}

	protected EntityInformation()
	{
	}
	
	public EntityState getState()
	{
		return entityState;
	}

	public void setState(EntityState state)
	{
		this.entityState = state;
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

	public Date getRemovalByUserTime()
	{
		return removalByUserTime;
	}

	public void setRemovalByUserTime(Date removalByUserTime)
	{
		this.removalByUserTime = removalByUserTime;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entityState == null) ? 0 : entityState.hashCode());
		result = prime * result
				+ ((removalByUserTime == null) ? 0 : removalByUserTime.hashCode());
		result = prime
				* result
				+ ((scheduledOperation == null) ? 0 : scheduledOperation.hashCode());
		result = prime
				* result
				+ ((scheduledOperationTime == null) ? 0 : scheduledOperationTime
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityInformation other = (EntityInformation) obj;
		if (entityState != other.entityState)
			return false;
		if (removalByUserTime == null)
		{
			if (other.removalByUserTime != null)
				return false;
		} else if (!removalByUserTime.equals(other.removalByUserTime))
			return false;
		if (scheduledOperation != other.scheduledOperation)
			return false;
		if (scheduledOperationTime == null)
		{
			if (other.scheduledOperationTime != null)
				return false;
		} else if (!scheduledOperationTime.equals(other.scheduledOperationTime))
			return false;
		return true;
	}
}
