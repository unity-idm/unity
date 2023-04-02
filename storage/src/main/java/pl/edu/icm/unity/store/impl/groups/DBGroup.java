/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBGroup.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
class DBGroup extends DBGroupBase
{
	public final String path;

	private DBGroup(Builder builder)
	{
		super(builder);
		this.path = builder.path;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(path);
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
		DBGroup other = (DBGroup) obj;
		return Objects.equals(path, other.path);
	}

	public static Builder builder()
	{
		return new Builder();
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder extends DBGroupBaseBuilder<Builder>
	{
		private String path;

		public Builder()
		{
		}

		public Builder withPath(String path)
		{
			this.path = path;
			return this;
		}

		public DBGroup build()
		{
			return new DBGroup(this);
		}
	}

}
