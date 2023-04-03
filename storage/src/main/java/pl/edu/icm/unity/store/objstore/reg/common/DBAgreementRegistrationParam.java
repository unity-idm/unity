/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.common.DBI18nString;

@JsonDeserialize(builder = DBAgreementRegistrationParam.Builder.class)
class DBAgreementRegistrationParam
{
	@JsonProperty("i18nText")
	final DBI18nString text;
	@JsonProperty("manatory")
	final boolean mandatory;

	private DBAgreementRegistrationParam(Builder builder)
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
		DBAgreementRegistrationParam other = (DBAgreementRegistrationParam) obj;
		return mandatory == other.mandatory && Objects.equals(text, other.text);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		@JsonProperty("i18nText")
		private DBI18nString text;
		@JsonProperty("manatory")
		private boolean mandatory;

		private Builder()
		{
		}

		public Builder withText(DBI18nString text)
		{
			this.text = text;
			return this;
		}

		public Builder withMandatory(boolean manatory)
		{
			this.mandatory = manatory;
			return this;
		}

		public DBAgreementRegistrationParam build()
		{
			return new DBAgreementRegistrationParam(this);
		}
	}

}
