/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.basic.RestI18nString;

@JsonDeserialize(builder = RestAgreementRegistrationParam.Builder.class)
public class RestAgreementRegistrationParam
{
	@JsonProperty("i18nText")
	public final RestI18nString text;
	@JsonProperty("manatory")
	public final boolean mandatory;

	private RestAgreementRegistrationParam(Builder builder)
	{
		this.text = builder.text;
		this.mandatory = builder.mandatory;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(mandatory, text);
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
		RestAgreementRegistrationParam other = (RestAgreementRegistrationParam) obj;
		return mandatory == other.mandatory && Objects.equals(text, other.text);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		@JsonProperty("i18nText")
		private RestI18nString text;
		@JsonProperty("manatory")
		private boolean mandatory;

		private Builder()
		{
		}

		public Builder withText(RestI18nString text)
		{
			this.text = text;
			return this;
		}

		public Builder withMandatory(boolean manatory)
		{
			this.mandatory = manatory;
			return this;
		}

		public RestAgreementRegistrationParam build()
		{
			return new RestAgreementRegistrationParam(this);
		}
	}

}
