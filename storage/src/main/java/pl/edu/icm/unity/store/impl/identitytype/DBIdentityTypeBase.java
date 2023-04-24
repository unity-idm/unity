/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identitytype;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBIdentityTypeBase.DBIdentityTypeBaseBuilder.class)
class DBIdentityTypeBase
{
	final String identityTypeProvider;
	final String identityTypeProviderSettings;
	final String description;
	final boolean selfModificable;
	final int minInstances;
	final int maxInstances;
	final int minVerifiedInstances;
	final DBEmailConfirmationConfiguration emailConfirmationConfiguration;

	protected DBIdentityTypeBase(DBIdentityTypeBaseBuilder<?> builder)
	{
		this.identityTypeProvider = builder.identityTypeProvider;
		this.identityTypeProviderSettings = builder.identityTypeProviderSettings;
		this.description = builder.description;
		this.selfModificable = builder.selfModificable;
		this.minInstances = builder.minInstances;
		this.maxInstances = builder.maxInstances;
		this.minVerifiedInstances = builder.minVerifiedInstances;
		this.emailConfirmationConfiguration = builder.emailConfirmationConfiguration;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(description, emailConfirmationConfiguration, identityTypeProvider,
				identityTypeProviderSettings, maxInstances, minInstances, minVerifiedInstances, selfModificable);
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
		DBIdentityTypeBase other = (DBIdentityTypeBase) obj;
		return Objects.equals(description, other.description)
				&& Objects.equals(emailConfirmationConfiguration, other.emailConfirmationConfiguration)
				&& Objects.equals(identityTypeProvider, other.identityTypeProvider)
				&& Objects.equals(identityTypeProviderSettings, other.identityTypeProviderSettings)
				&& maxInstances == other.maxInstances && minInstances == other.minInstances
				&& minVerifiedInstances == other.minVerifiedInstances && selfModificable == other.selfModificable;
	}

	public static DBIdentityTypeBaseBuilder<?> builder()
	{
		return new DBIdentityTypeBaseBuilder<>();
	}

	public static class DBIdentityTypeBaseBuilder<T extends DBIdentityTypeBaseBuilder<?>>
	{
		private String identityTypeProvider;
		private String identityTypeProviderSettings;
		private String description;
		private boolean selfModificable;
		private int minInstances;
		private int maxInstances;
		private int minVerifiedInstances;
		private DBEmailConfirmationConfiguration emailConfirmationConfiguration;

		protected DBIdentityTypeBaseBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withIdentityTypeProvider(String identityTypeProvider)
		{
			this.identityTypeProvider = identityTypeProvider;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withIdentityTypeProviderSettings(String identityTypeProviderSettings)
		{
			this.identityTypeProviderSettings = identityTypeProviderSettings;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withDescription(String description)
		{
			this.description = description;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withSelfModificable(boolean selfModificable)
		{
			this.selfModificable = selfModificable;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withMinInstances(int minInstances)
		{
			this.minInstances = minInstances;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withMaxInstances(int maxInstances)
		{
			this.maxInstances = maxInstances;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withMinVerifiedInstances(int minVerifiedInstances)
		{
			this.minVerifiedInstances = minVerifiedInstances;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withEmailConfirmationConfiguration(DBEmailConfirmationConfiguration emailConfirmationConfiguration)
		{
			this.emailConfirmationConfiguration = emailConfirmationConfiguration;
			return (T) this;
		}

		public DBIdentityTypeBase build()
		{
			return new DBIdentityTypeBase(this);
		}
	}

}
