/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBurlQueryPrefillConfig.Builder.class)
class DBurlQueryPrefillConfig
{
	public final String paramName;
	public final String mode;

	private DBurlQueryPrefillConfig(Builder builder)
	{
		this.paramName = builder.paramName;
		this.mode = builder.mode;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(mode, paramName);
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
		DBurlQueryPrefillConfig other = (DBurlQueryPrefillConfig) obj;
		return Objects.equals(mode, other.mode) && Objects.equals(paramName, other.paramName);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String paramName;
		private String mode;

		private Builder()
		{
		}

		public Builder withParamName(String paramName)
		{
			this.paramName = paramName;
			return this;
		}

		public Builder withMode(String mode)
		{
			this.mode = mode;
			return this;
		}

		public DBurlQueryPrefillConfig build()
		{
			return new DBurlQueryPrefillConfig(this);
		}
	}

}
