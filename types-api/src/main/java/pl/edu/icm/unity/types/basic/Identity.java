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
	private Date creationTs = new Date();
	private Date updateTs = new Date();
	private String comparableValue;
	
	public Identity(String type, String value, long entityId, String comparableValue)
	{
		super(type, value);
		this.entityId = entityId;
		this.comparableValue = comparableValue;
	}

	/**
	 * Allows for creating {@link Identity} out of {@link IdentityParam}.
	 * @param idParam
	 * @param entityId
	 * @param comparableValue
	 */
	public Identity(IdentityParam idParam, long entityId, String comparableValue)
	{
		super(idParam.getTypeId(), idParam.getValue());
		this.entityId = entityId;
		this.comparableValue = comparableValue;
		
		setConfirmationInfo(idParam.getConfirmationInfo());
		setMetadata(idParam.getMetadata());
		setRemoteIdp(idParam.getRemoteIdp());
		setRealm(idParam.getRealm());
		setTarget(idParam.getTarget());
		setTranslationProfile(idParam.getTranslationProfile());
	}
	
	@JsonCreator
	public Identity(ObjectNode src)
	{
		super(src);
		fromJson(src);
	}

	/**
	 * Partial creation from JSON (used by RDBMS storage)
	 * @param type
	 * @param entityId
	 * @param comparableValue
	 * @param src
	 */
	public Identity(String type, long entityId, ObjectNode src)
	{
		super(type, src);
		fromJsonBase(src);
		this.entityId = entityId;
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

	private final void fromJson(ObjectNode src)
	{
		fromJsonBase(src);
		setEntityId(src.get("entityId").asLong());
	}
	
	@Override
	public Identity clone()
	{
		ObjectNode json = toJson();
		return new Identity(json);
	}
	
	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = super.toJson();
		main.put("entityId", getEntityId());
		return main;
	}

	@Override
	public ObjectNode toJsonBase()
	{
		ObjectNode main = super.toJsonBase();
		main.put("comparableValue", getComparableValue());
		main.put("creationTs", getCreationTs().getTime());
		main.put("updateTs", getUpdateTs().getTime());
		return main;
	}
	
	private void fromJsonBase(ObjectNode main)
	{
		JsonNode cmpVal = main.get("comparableValue");
		if (cmpVal.isNull())
			throw new IllegalArgumentException("Got identity without comparable value, what is invalid: "
					+ main.toString());
		setComparableValue(main.get("comparableValue").asText());
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
