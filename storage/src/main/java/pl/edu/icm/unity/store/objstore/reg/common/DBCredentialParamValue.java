/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBCredentialParamValue.Builder.class)
public class DBCredentialParamValue
{
	public final String credentialId;
	public final String secrets;

	private DBCredentialParamValue(Builder builder)
	{
		this.credentialId = builder.credentialId;
		this.secrets = builder.secrets;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(credentialId, secrets);
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
		DBCredentialParamValue other = (DBCredentialParamValue) obj;
		return Objects.equals(credentialId, other.credentialId) && Objects.equals(secrets, other.secrets);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String credentialId;
		private String secrets;

		private Builder()
		{
		}

		public Builder withCredentialId(String credentialId)
		{
			this.credentialId = credentialId;
			return this;
		}

		public Builder withSecrets(String secrets)
		{
			this.secrets = secrets;
			return this;
		}

		public DBCredentialParamValue build()
		{
			return new DBCredentialParamValue(this);
		}
	}

}
