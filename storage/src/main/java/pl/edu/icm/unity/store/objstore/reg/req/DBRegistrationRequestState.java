/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.req;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.objstore.reg.common.DBUserRequestState;

@JsonDeserialize(builder = DBRegistrationRequestState.Builder.class)
class DBRegistrationRequestState extends DBUserRequestState<DBRegistrationRequest>
{
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("CreatedEntityId")
	final Long createdEntityId;

	private DBRegistrationRequestState(Builder builder)
	{
		super(builder);
		this.createdEntityId = builder.createdEntityId;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(createdEntityId);
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
		DBRegistrationRequestState other = (DBRegistrationRequestState) obj;
		return Objects.equals(createdEntityId, other.createdEntityId);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestUserRequestStateBuilder<DBRegistrationRequest, Builder>
	{
		@JsonProperty("CreatedEntityId")
		private Long createdEntityId;

		public Builder withCreatedEntityId(Long createdEntityId)
		{
			this.createdEntityId = createdEntityId;
			return this;
		}

		public DBRegistrationRequestState build()
		{
			return new DBRegistrationRequestState(this);
		}
	}
}
