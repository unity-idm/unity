/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBFormLayout.Builder.class)
public class DBFormLayout
{
	public final List<DBFormElement> elements;

	private DBFormLayout(Builder builder)
	{
		this.elements = Optional.ofNullable(builder.elements)
				.map(List::copyOf)
				.orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(elements);
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
		DBFormLayout other = (DBFormLayout) obj;
		return Objects.equals(elements, other.elements);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<DBFormElement> elements = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withElements(List<DBFormElement> elements)
		{
			this.elements = Optional.ofNullable(elements)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public DBFormLayout build()
		{
			return new DBFormLayout(this);
		}
	}

}
