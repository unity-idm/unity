/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.policyAgreement;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.basic.RestI18nString;

@JsonDeserialize(builder = RestPolicyAgreementConfiguration.Builder.class)
public class RestPolicyAgreementConfiguration
{
	public final List<Long> documentsIdsToAccept;
	public final String presentationType;
	public final RestI18nString text;

	private RestPolicyAgreementConfiguration(Builder builder)
	{
		this.documentsIdsToAccept = Optional.ofNullable(builder.documentsIdsToAccept)
				.map(List::copyOf)
				.orElse(null);
		this.presentationType = builder.presentationType;
		this.text = builder.text;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(documentsIdsToAccept, presentationType, text);
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
		RestPolicyAgreementConfiguration other = (RestPolicyAgreementConfiguration) obj;
		return Objects.equals(documentsIdsToAccept, other.documentsIdsToAccept)
				&& Objects.equals(presentationType, other.presentationType) && Objects.equals(text, other.text);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<Long> documentsIdsToAccept = Collections.emptyList();
		private String presentationType;
		private RestI18nString text;

		private Builder()
		{
		}

		public Builder withDocumentsIdsToAccept(List<Long> documentsIdsToAccept)
		{
			this.documentsIdsToAccept = Optional.ofNullable(documentsIdsToAccept)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public Builder withPresentationType(String presentationType)
		{
			this.presentationType = presentationType;
			return this;
		}

		public Builder withText(RestI18nString text)
		{
			this.text = text;
			return this;
		}

		public RestPolicyAgreementConfiguration build()
		{
			return new RestPolicyAgreementConfiguration(this);
		}
	}

}
