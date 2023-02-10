/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.policyAgreement;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


import java.util.Collections;

@JsonDeserialize(builder = RestPolicyAgreementDecision.Builder.class)
public class RestPolicyAgreementDecision
{
	public final String acceptanceStatus;
	public final List<Long> documentsIdsToAccept;

	private RestPolicyAgreementDecision(Builder builder)
	{
		this.acceptanceStatus = builder.acceptanceStatus;
		this.documentsIdsToAccept = builder.documentsIdsToAccept;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(acceptanceStatus, documentsIdsToAccept);
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
		RestPolicyAgreementDecision other = (RestPolicyAgreementDecision) obj;
		return Objects.equals(acceptanceStatus, other.acceptanceStatus)
				&& Objects.equals(documentsIdsToAccept, other.documentsIdsToAccept);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String acceptanceStatus;
		private List<Long> documentsIdsToAccept = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withAcceptanceStatus(String acceptanceStatus)
		{
			this.acceptanceStatus = acceptanceStatus;
			return this;
		}

		public Builder withDocumentsIdsToAccept(List<Long> documentsIdsToAccept)
		{
			this.documentsIdsToAccept = documentsIdsToAccept;
			return this;
		}

		public RestPolicyAgreementDecision build()
		{
			return new RestPolicyAgreementDecision(this);
		}
	}

}
