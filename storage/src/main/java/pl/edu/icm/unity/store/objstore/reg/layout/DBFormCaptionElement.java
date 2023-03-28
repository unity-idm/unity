/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.layout;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.DBI18nString;

@JsonDeserialize(builder = DBFormCaptionElement.Builder.class)
public class DBFormCaptionElement extends DBFormElement
{
	public final DBI18nString value;

	private DBFormCaptionElement(Builder builder)
	{
		super(builder);
		this.value = builder.value;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(value);
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
		DBFormCaptionElement other = (DBFormCaptionElement) obj;
		return Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestFormElementBuilder<Builder>
	{
		private static final String CLASS_TYPE = "pl.edu.icm.unity.types.registration.layout.FormCaptionElement";
		private static final String TYPE = "CAPTION";
		private DBI18nString value;

		private Builder()
		{
			super(CLASS_TYPE);
			withType(TYPE);
			withFormContentsRelated(false);
		}

		public Builder withValue(DBI18nString value)
		{
			this.value = value;
			return this;
		}

		public DBFormCaptionElement build()
		{
			return new DBFormCaptionElement(this);
		}
	}

}
