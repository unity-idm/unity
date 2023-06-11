/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.utils.JsonUtil;


/**
 * Represents an identity type and value. This class is useful to address existing identity as a parameter.
 * <p>
 * Optionally a target can be set. Then the identity can be resolved for the specified receiver.
 * 
 * @author K. Benedyczak
 */
public class IdentityTaV
{
	private String typeId;
	protected String value;
	protected String target;
	protected String realm;
	
	
	public IdentityTaV(String type, String value) 
	{
		this.typeId = type;
		this.value = value;
		validateInitialization();
	}

	public IdentityTaV(String type, String value, String target, String realm) 
	{
		this(type, value);
		this.target = target;
		this.realm = realm;
	}

	@JsonCreator
	public IdentityTaV(ObjectNode src)
	{
		fromJson(src);
	}

	public IdentityTaV(String type, ObjectNode src)
	{
		this.typeId = type;
		fromJsonBase(src);
	}
	
	public String getValue()
	{
		return value;
	}
	
	public String getTypeId()
	{
		return typeId;
	}

	public void setTypeId(String type)
	{
		this.typeId = type;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getTarget()
	{
		return target;
	}

	public void setTarget(String target)
	{
		this.target = target;
	}
	
	/**
	 * @return authentication realm in which this identity is applicable or null when it is not realm specific. 
	 */
	public String getRealm()
	{
		return realm;
	}

	public void setRealm(String realm)
	{
		this.realm = realm;
	}
	
	private final void fromJson(ObjectNode src)
	{
		setTypeId(src.get("typeId").asText());
		fromJsonBase(src);
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = toJsonBase();
		main.put("typeId", getTypeId());
		return main;
	}

	public ObjectNode toJsonBase()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		
		main.put("value", getValue());
		
		if (getRealm() != null)
			main.put("realm", getRealm());
		if (getTarget() != null)
			main.put("target", getTarget());
		return main;
	}
	
	private final void fromJsonBase(ObjectNode main)
	{
		setValue(JsonUtil.getNullable(main, "value"));
		setRealm(JsonUtil.getNullable(main, "realm"));
		setTarget(JsonUtil.getNullable(main, "target"));
	}

	
	private void validateInitialization()
	{
		if (typeId == null)
			throw new IllegalArgumentException("Identity type must not be null");
		if (value == null)
			throw new IllegalArgumentException("Identity value must not be null");
	}
	
	/**
	 * @return full String representation
	 */
	public String toString()
	{
		StringBuilder base = new StringBuilder("[" + typeId + "] " + value);
		if (target != null)
			base.append(" for " + target);
		if (realm != null)
			base.append("@" + realm);
		return base.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		IdentityTaV other = (IdentityTaV) obj;
		if (realm == null)
		{
			if (other.realm != null)
				return false;
		} else if (!realm.equals(other.realm))
			return false;
		if (target == null)
		{
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (typeId == null)
		{
			if (other.typeId != null)
				return false;
		} else if (!typeId.equals(other.typeId))
			return false;
		if (value == null)
		{
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
