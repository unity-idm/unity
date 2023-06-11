/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Specialization of {@link UserRequestState} for {@link EnquiryResponse}s.
 * @author K. Benedyczak
 */
public class EnquiryResponseState extends UserRequestState<EnquiryResponse>
{
	private long entityId;

	public EnquiryResponseState()
	{
	}

	@JsonCreator
	public EnquiryResponseState(ObjectNode root)
	{
		super(root);
		entityId = root.get("EntityId").asLong();
	}

	public long getEntityId()
	{
		return entityId;
	}

	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}

	@Override
	public String toString()
	{
		return "EnquiryResponseState [entityId=" + entityId + "]";
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = super.toJson();
		root.put("EntityId", entityId);
		return root;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (entityId ^ (entityId >>> 32));
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
		EnquiryResponseState other = (EnquiryResponseState) obj;
		if (entityId != other.entityId)
			return false;
		return true;
	}

	@Override
	protected EnquiryResponse parseRequestFromJson(ObjectNode root)
	{
		return new EnquiryResponse(root);
	}
}
