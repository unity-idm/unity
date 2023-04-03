/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identitytype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBIdentityType.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
class DBIdentityType extends DBIdentityTypeBase
{
	public final String name;

	private DBIdentityType(Builder builder)
	{
		super(builder);
		this.name = builder.name;

	}

	public static Builder builder()
	{
		return new Builder();
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder extends DBIdentityTypeBaseBuilder<Builder>
	{
		private String name;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public DBIdentityType build()
		{
			return new DBIdentityType(this);
		}
	}

}
