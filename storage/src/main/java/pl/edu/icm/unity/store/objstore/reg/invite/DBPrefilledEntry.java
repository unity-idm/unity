/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBPrefilledEntry.Builder.class)
public class DBPrefilledEntry<T>
{
	public final T entry;
	public final String mode;

	private DBPrefilledEntry(Builder<T> builder)
	{
		this.entry = builder.entry;
		this.mode = builder.mode;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entry, mode);
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
		DBPrefilledEntry<?> other = (DBPrefilledEntry<?>) obj;
		return Objects.equals(entry, other.entry) && Objects.equals(mode, other.mode);
	}

	public static final class Builder<T>
	{
		private T entry;
		private String mode;

		public Builder()
		{
		}

		public Builder<T> withEntry(T entry)
		{
			this.entry = entry;
			return this;
		}

		public Builder<T> withMode(String mode)
		{
			this.mode = mode;
			return this;
		}

		public DBPrefilledEntry<T> build()
		{
			return new DBPrefilledEntry<T>(this);
		}
	}

}
