/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.JsonUtil;

/**
 * Specialization of {@link UserRequestState} for {@link RegistrationRequest}s.
 * @author K. Benedyczak
 */
public class RegistrationRequestState extends UserRequestState<RegistrationRequest>
{
	private Long createdEntityId;

	public RegistrationRequestState()
	{
	}

	@JsonCreator
	public RegistrationRequestState(ObjectNode root)
	{
		super(root);
		fromJson(root);
	}

	@Override
	protected RegistrationRequest parseRequestFromJson(ObjectNode root)
	{
		return new RegistrationRequest(root);
	}
	
	/**
	 * @return for all accepted requests return the entityId of the entity created by the registration. 
	 * Otherwise null.
	 */
	public Long getCreatedEntityId()
	{
		return createdEntityId;
	}

	public void setCreatedEntityId(Long createdEntityId)
	{
		this.createdEntityId = createdEntityId;
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = super.toJson();
		if (createdEntityId != null)
			root.put("CreatedEntityId", createdEntityId);
		return root;
	}

	
	private void fromJson(ObjectNode root)
	{
		if (JsonUtil.notNull(root, "CreatedEntityId"))
			createdEntityId = root.get("CreatedEntityId").asLong();
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((createdEntityId == null) ? 0 : createdEntityId.hashCode());
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
		RegistrationRequestState other = (RegistrationRequestState) obj;
		if (createdEntityId == null)
		{
			if (other.createdEntityId != null)
				return false;
		} else if (!createdEntityId.equals(other.createdEntityId))
			return false;
		return true;
	}
}
