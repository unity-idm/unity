/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;

/**
 * Stores information about entity, besides its identities, credentials and basic information as id.
 * @author K. Benedyczak
 */
public class EntityInformation
{
	private Long id;
	private EntityState entityState = EntityState.valid;
	private Date scheduledOperationTime;
	private EntityScheduledOperation scheduledOperation;
	private Date removalByUserTime;
	
	public EntityInformation()
	{
	}

	public EntityInformation(long id)
	{
		this.id = id;
	}

	public EntityInformation(ObjectNode json)
	{
		fromJson(json);
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

	public Long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public void setEntityState(EntityState entityState)
	{
		this.entityState = entityState;
	}

	public EntityState getEntityState()
	{
		return entityState;
	}

	@Override
	public String toString()
	{
		return "EntityInformation [id=" + id + ", entityState=" + entityState + "]";
	}

	private void fromJson(ObjectNode src)
	{
		fromJsonBase(src);
		id = src.get("entityId").asLong();
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = toJsonBase();
		main.put("entityId", getId());
		return main;
	}

	public ObjectNode toJsonBase()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("state", getState().name());
		if (getScheduledOperationTime() != null)
			main.put("ScheduledOperationTime", getScheduledOperationTime().getTime());
		if (getScheduledOperation() != null)
			main.put("ScheduledOperation", getScheduledOperation().name());
		if (getRemovalByUserTime() != null)
			main.put("RemovalByUserTime", getRemovalByUserTime().getTime());
		return main;
	}
	
	public void fromJsonBase(ObjectNode main)
	{
		String stateStr = main.get("state").asText();
		setState(EntityState.valueOf(stateStr));
		
		if (main.has("ScheduledOperationTime"))
			setScheduledOperationTime(new Date(main.get("ScheduledOperationTime").asLong()));
		if (main.has("ScheduledOperation"))
			setScheduledOperation(EntityScheduledOperation.valueOf(
					main.get("ScheduledOperation").asText()));
		if (main.has("RemovalByUserTime"))
			setRemovalByUserTime(new Date(main.get("RemovalByUserTime").asLong()));		
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entityState == null) ? 0 : entityState.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result
				+ ((removalByUserTime == null) ? 0 : removalByUserTime.hashCode());
		result = prime * result + ((scheduledOperation == null) ? 0
				: scheduledOperation.hashCode());
		result = prime * result + ((scheduledOperationTime == null) ? 0
				: scheduledOperationTime.hashCode());
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
		if (id != other.id)
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
