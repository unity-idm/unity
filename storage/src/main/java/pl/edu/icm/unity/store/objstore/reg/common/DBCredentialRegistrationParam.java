/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBCredentialRegistrationParam.Builder.class)
public class DBCredentialRegistrationParam
{
	public final String credentialName;
	public final String label;
	public final String description;

	private DBCredentialRegistrationParam(Builder builder)
	{
		this.credentialName = builder.credentialName;
		this.label = builder.label;
		this.description = builder.description;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(credentialName, description, label);
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
		DBCredentialRegistrationParam other = (DBCredentialRegistrationParam) obj;
		return Objects.equals(credentialName, other.credentialName) && Objects.equals(description, other.description)
				&& Objects.equals(label, other.label);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String credentialName;
		private String label;
		private String description;

		private Builder()
		{
		}

		public Builder withCredentialName(String credentialName)
		{
			this.credentialName = credentialName;
			return this;
		}

		public Builder withLabel(String label)
		{
			this.label = label;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public DBCredentialRegistrationParam build()
		{
			return new DBCredentialRegistrationParam(this);
		}
	}

}
