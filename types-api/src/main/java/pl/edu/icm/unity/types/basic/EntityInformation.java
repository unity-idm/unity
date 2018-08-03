/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import java.util.Objects;

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

	@JsonCreator
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
	
	@Override
	public EntityInformation clone()
	{
		ObjectNode json = toJson();
		return new EntityInformation(json);
	}
	
	private void fromJson(ObjectNode src)
	{
		fromJsonBase(src);
		
		id = src.hasNonNull("entityId") ? src.get("entityId").asLong() : null;
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
	public boolean equals(final Object other)
	{
		if (!(other instanceof EntityInformation))
			return false;
		EntityInformation castOther = (EntityInformation) other;
		return Objects.equals(id, castOther.id) && Objects.equals(entityState, castOther.entityState)
				&& Objects.equals(scheduledOperationTime, castOther.scheduledOperationTime)
				&& Objects.equals(scheduledOperation, castOther.scheduledOperation)
				&& Objects.equals(removalByUserTime, castOther.removalByUserTime);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, entityState, scheduledOperationTime, scheduledOperation, removalByUserTime);
	}
}
