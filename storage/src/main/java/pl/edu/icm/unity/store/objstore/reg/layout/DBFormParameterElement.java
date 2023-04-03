/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = DBFormParameterElement.Builder.class)
public class DBFormParameterElement extends DBFormElement
{
	public final int index;

	private DBFormParameterElement(Builder builder)
	{
		super(builder);
		this.index = builder.index;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(index);
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
		DBFormParameterElement other = (DBFormParameterElement) obj;
		return index == other.index;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestFormElementBuilder<Builder>
	{
		private static final String CLASS_TYPE = "pl.edu.icm.unity.types.registration.layout.FormParameterElement";
		private int index;

		private Builder()
		{
			super(CLASS_TYPE);
			withFormContentsRelated(true);
		}

		public Builder withIndex(int index)
		{
			this.index = index;
			return this;
		}

		public DBFormParameterElement build()
		{
			return new DBFormParameterElement(this);
		}
	}

}
