/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.time.Instant;

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
