/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eresp;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.objstore.reg.common.DBUserRequestState;

@JsonDeserialize(builder = DBEnquiryResponseState.Builder.class)
public class DBEnquiryResponseState extends DBUserRequestState<DBEnquiryResponse>
{
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("EntityId")
	final Long entityId;

	private DBEnquiryResponseState(Builder builder)
	{
		super(builder);
		this.entityId = builder.entityId;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(entityId);
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
		DBEnquiryResponseState other = (DBEnquiryResponseState) obj;
		return Objects.equals(entityId, other.entityId);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestUserRequestStateBuilder<DBEnquiryResponse, Builder>
	{
		@JsonProperty("EntityId")
		private Long entityId;

		public Builder withEntityId(Long createdEntityId)
		{
			this.entityId = createdEntityId;
			return this;
		}

		public DBEnquiryResponseState build()
		{
			return new DBEnquiryResponseState(this);
		}
	}
}
