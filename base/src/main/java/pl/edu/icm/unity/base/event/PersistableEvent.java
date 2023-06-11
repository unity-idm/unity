/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.event;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;

/**
 * Describes execution of operation with its context. Suitable for auditing, 
 * time measurement, logging or sending notifications. 
 * @author K. Benedyczak
 */
public class PersistableEvent implements Event
{
	private String trigger;
	private Long invokerEntity;
	private Date timestamp;
	private String contents;

	public PersistableEvent(Enum<?> trigger, String contents)
	{
		this(trigger.toString(), -1l, new Date(), contents);
	}
	
	public PersistableEvent(Enum<?> trigger)
	{
		this(trigger.toString());
	}

	public PersistableEvent(String trigger)
	{
		this(trigger, -1l, new Date(), null);
	}
	
	public PersistableEvent(String trigger, Long invokerEntity, Date timestamp)
	{
		this(trigger, invokerEntity, timestamp, null);
	}

	public PersistableEvent(String trigger, Long invokerEntity, Date timestamp, String contents)
	{
		this.trigger = trigger;
		this.invokerEntity = invokerEntity;
		this.timestamp = timestamp;
		this.contents = contents;
	}

	@JsonCreator
	public PersistableEvent(ObjectNode json)
	{
		fromJson(json);
	}

	public String getTrigger()
	{
		return trigger;
	}

	public void setTrigger(String trigger)
	{
		this.trigger = trigger;
	}

	public Long getInvokerEntity()
	{
		return invokerEntity;
	}

	public void setInvokerEntity(Long invokerEntity)
	{
		this.invokerEntity = invokerEntity;
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getContents()
	{
		return contents;
	}

	public void setContents(String contents)
	{
		this.contents = contents;
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("trigger", getTrigger());
		main.put("contents", getContents());
		main.put("invokerEntity", getInvokerEntity());
		main.put("timestamp", getTimestamp().getTime());
		return main;
	}
	
	private void fromJson(ObjectNode main)
	{
		trigger = main.get("trigger").asText();
		contents = main.get("contents").asText();
		invokerEntity = main.get("invokerEntity").asLong();
		long ts = main.get("timestamp").asLong();
		timestamp = new Date(ts);
	}
	
	
	@Override
	public String toString()
	{
		return "[trigger=" + trigger + ", invokerEntity=" + invokerEntity
				+ ", timestamp=" + timestamp + ", contents=" + contents + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((trigger == null) ? 0 : trigger.hashCode());
		result = prime * result + ((contents == null) ? 0 : contents.hashCode());
		result = prime * result + ((invokerEntity == null) ? 0 : invokerEntity.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
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
		PersistableEvent other = (PersistableEvent) obj;
		if (trigger == null)
		{
			if (other.trigger != null)
				return false;
		} else if (!trigger.equals(other.trigger))
			return false;
		if (contents == null)
		{
			if (other.contents != null)
				return false;
		} else if (!contents.equals(other.contents))
			return false;
		if (invokerEntity == null)
		{
			if (other.invokerEntity != null)
				return false;
		} else if (!invokerEntity.equals(other.invokerEntity))
			return false;
		if (timestamp == null)
		{
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		return true;
	}
}
