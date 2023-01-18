/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.layout;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.basic.RestI18nString;

@JsonDeserialize(builder = RestFormCaptionElement.Builder.class)
public class RestFormCaptionElement extends RestFormElement
{
	public final RestI18nString value;

	private RestFormCaptionElement(Builder builder)
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
		RestFormCaptionElement other = (RestFormCaptionElement) obj;
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
		private RestI18nString value;

		private Builder()
		{
			super(CLASS_TYPE);
			withType(TYPE);
			withFormContentsRelated(false);
		}

		public Builder withValue(RestI18nString value)
		{
			this.value = value;
			return this;
		}

		public RestFormCaptionElement build()
		{
			return new RestFormCaptionElement(this);
		}
	}

}
