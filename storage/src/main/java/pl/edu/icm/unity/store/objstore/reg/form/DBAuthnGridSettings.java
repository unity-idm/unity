/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBAuthnGridSettings.Builder.class)
class DBAuthnGridSettings
{
	public final boolean searchable;
	public final int height;

	private DBAuthnGridSettings(DBAuthnGridSettings.Builder builder)
	{
		this.searchable = builder.searchable;
		this.height = builder.height;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(height, searchable);
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
		DBAuthnGridSettings other = (DBAuthnGridSettings) obj;
		return height == other.height && searchable == other.searchable;
	}

	public static DBAuthnGridSettings.Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean searchable;
		private int height;

		private Builder()
		{
		}

		public DBAuthnGridSettings.Builder withSearchable(boolean searchable)
		{
			this.searchable = searchable;
			return this;
		}

		public DBAuthnGridSettings.Builder withHeight(int height)
		{
			this.height = height;
			return this;
		}

		public DBAuthnGridSettings build()
		{
			return new DBAuthnGridSettings(this);
		}
	}

}