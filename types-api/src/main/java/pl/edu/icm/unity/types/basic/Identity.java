/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.types.NamedObject;

/**
 * Represents an identity with full information as returned from the engine.
 * 
 * @author K. Benedyczak
 */
public class Identity extends IdentityParam implements NamedObject
{
	private long entityId;
	private Date creationTs;
	private Date updateTs;
	private String comparableValue;
	
	public Identity(String type, String value, Long entityId, String comparableValue)
	{
		super(type, value);
		this.entityId = entityId;
		this.comparableValue = comparableValue;
	}
	
	@JsonCreator
	public Identity(ObjectNode src)
	{
		super(src);
	}

	public Identity(String type, Long entityId, String comparableValue, ObjectNode src)
	{
		super(type, src);
		this.entityId = entityId;
		this.comparableValue = comparableValue;
	}

	public long getEntityId()
	{
		return entityId;
	}

	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}

	public Date getCreationTs()
	{
		return creationTs;
	}

	public void setCreationTs(Date creationTs)
	{
		this.creationTs = creationTs;
	}

	public Date getUpdateTs()
	{
		return updateTs;
	}

	public void setUpdateTs(Date updateTs)
	{
		this.updateTs = updateTs;
	}

	public String getComparableValue()
	{
		return comparableValue;
	}

	public void setComparableValue(String comparableValue)
	{
		this.comparableValue = comparableValue;
	}

	@Override
	public String getName()
	{
		return getComparableValue();
	}

	@Override
	protected void fromJson(ObjectNode src)
	{
		super.fromJson(src);
		JsonNode cmpVal = src.get("comparableValue");
		if (cmpVal.isNull())
			throw new IllegalArgumentException("Got identity without comparable value, what is invalid: "
					+ src.toString());
		setComparableValue(src.get("comparableValue").asText());
		setEntityId(src.get("entityId").asLong());
	}
	
	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = super.toJson();
		main.put("comparableValue", getComparableValue());
		main.put("entityId", getEntityId());
		return main;
	}

	@Override
	public ObjectNode toJsonBase()
	{
		ObjectNode main = super.toJsonBase();
		main.put("creationTs", getCreationTs().getTime());
		main.put("updateTs", getUpdateTs().getTime());
		return main;
	}
	
	@Override
	public void fromJsonBase(ObjectNode main)
	{
		super.fromJsonBase(main);
		if (main.has("creationTs"))
			setCreationTs(new Date(main.get("creationTs").asLong()));
		if (main.has("updateTs"))
			setUpdateTs(new Date(main.get("updateTs").asLong()));
	}
	
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((comparableValue == null) ? 0 : comparableValue.hashCode());
		result = prime * result + ((creationTs == null) ? 0 : creationTs.hashCode());
		result = prime * result + (int) (entityId ^ (entityId >>> 32));
		result = prime * result + ((updateTs == null) ? 0 : updateTs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Identity other = (Identity) obj;
		if (comparableValue == null)
		{
			if (other.comparableValue != null)
				return false;
		} else if (!comparableValue.equals(other.comparableValue))
			return false;
		if (creationTs == null)
		{
			if (other.creationTs != null)
				return false;
		} else if (!creationTs.equals(other.creationTs))
			return false;
		if (entityId != other.entityId)
			return false;
		if (updateTs == null)
		{
			if (other.updateTs != null)
				return false;
		} else if (!updateTs.equals(other.updateTs))
			return false;
		return true;
	}
}
