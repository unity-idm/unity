/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.time.Instant;
import java.util.Objects;

class UserIdentity
{
	final Instant creationTs;
	final Instant updateTs;
	final String typeId;
	final String value;

	private UserIdentity(Builder builder)
	{
		this.creationTs = builder.creationTs;
		this.updateTs = builder.updateTs;
		this.typeId = builder.typeId;
		this.value = builder.value;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(creationTs, typeId, updateTs, value);
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
		UserIdentity other = (UserIdentity) obj;
		return Objects.equals(creationTs, other.creationTs) && Objects.equals(typeId, other.typeId)
				&& Objects.equals(updateTs, other.updateTs) && Objects.equals(value, other.value);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private Instant creationTs;
		private Instant updateTs;
		private String typeId;
		private String value;

		private Builder()
		{
		}

		Builder withCreationTs(Instant creationTs)
		{
			this.creationTs = creationTs;
			return this;
		}

		Builder withUpdateTs(Instant updateTs)
		{
			this.updateTs = updateTs;
			return this;
		}

		Builder withTypeId(String typeId)
		{
			this.typeId = typeId;
			return this;
		}

		Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		UserIdentity build()
		{
			return new UserIdentity(this);
		}
	}

}
