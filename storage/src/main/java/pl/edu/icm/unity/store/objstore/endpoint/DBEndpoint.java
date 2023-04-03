/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.endpoint;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBEndpoint.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
class DBEndpoint
{
	final String name;
	final String typeId;
	final String contextAddress;
	final DBEndpointConfiguration configuration;
	final long revision;
	final String status;

	private DBEndpoint(Builder builder)
	{
		this.name = builder.name;
		this.typeId = builder.typeId;
		this.contextAddress = builder.contextAddress;
		this.configuration = builder.configuration;
		this.revision = builder.revision;
		this.status = builder.status;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(configuration, contextAddress, name, revision, status, typeId);
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
		DBEndpoint other = (DBEndpoint) obj;
		return Objects.equals(configuration, other.configuration)
				&& Objects.equals(contextAddress, other.contextAddress) && Objects.equals(name, other.name)
				&& revision == other.revision && Objects.equals(status, other.status)
				&& Objects.equals(typeId, other.typeId);
	}

	public static Builder builder()
	{
		return new Builder();
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder
	{
		private String name;
		private String typeId;
		private String contextAddress;
		private DBEndpointConfiguration configuration;
		private long revision;
		private String status;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withTypeId(String typeId)
		{
			this.typeId = typeId;
			return this;
		}

		public Builder withContextAddress(String contextAddress)
		{
			this.contextAddress = contextAddress;
			return this;
		}

		public Builder withConfiguration(DBEndpointConfiguration configuration)
		{
			this.configuration = configuration;
			return this;
		}

		public Builder withRevision(long revision)
		{
			this.revision = revision;
			return this;
		}

		public Builder withStatus(String status)
		{
			this.status = status;
			return this;
		}

		public DBEndpoint build()
		{
			return new DBEndpoint(this);
		}
	}

}
