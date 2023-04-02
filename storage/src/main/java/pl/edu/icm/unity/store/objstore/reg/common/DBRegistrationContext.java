/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBRegistrationContext.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBRegistrationContext
{
	public final boolean isOnIdpEndpoint;
	public final String triggeringMode;

	private DBRegistrationContext(Builder builder)
	{
		this.isOnIdpEndpoint = builder.isOnIdpEndpoint;
		this.triggeringMode = builder.triggeringMode;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(isOnIdpEndpoint, triggeringMode);
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
		DBRegistrationContext other = (DBRegistrationContext) obj;
		return isOnIdpEndpoint == other.isOnIdpEndpoint && Objects.equals(triggeringMode, other.triggeringMode);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder
	{
		private boolean isOnIdpEndpoint;
		private String triggeringMode;

		private Builder()
		{
		}

		public Builder withIsOnIdpEndpoint(boolean isOnIdpEndpoint)
		{
			this.isOnIdpEndpoint = isOnIdpEndpoint;
			return this;
		}

		public Builder withTriggeringMode(String triggeringMode)
		{
			this.triggeringMode = triggeringMode;
			return this;
		}

		public DBRegistrationContext build()
		{
			return new DBRegistrationContext(this);
		}
	}

}
