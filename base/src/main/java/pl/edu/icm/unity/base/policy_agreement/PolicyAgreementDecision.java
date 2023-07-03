/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.policy_agreement;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PolicyAgreementDecision
{
	public final PolicyAgreementAcceptanceStatus acceptanceStatus;
	public final List<Long> documentsIdsToAccept;

	@JsonCreator
	public PolicyAgreementDecision(@JsonProperty("decision") PolicyAgreementAcceptanceStatus decision,
			@JsonProperty("documentsIdsToAccept") List<Long> documentsIdsToAccept)
	{
		this.acceptanceStatus = decision;
		this.documentsIdsToAccept = Collections
				.unmodifiableList(documentsIdsToAccept == null ? Collections.emptyList() : documentsIdsToAccept);
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof PolicyAgreementDecision))
			return false;
		PolicyAgreementDecision castOther = (PolicyAgreementDecision) other;
		return Objects.equals(acceptanceStatus, castOther.acceptanceStatus)
				&& Objects.equals(documentsIdsToAccept, castOther.documentsIdsToAccept);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(acceptanceStatus, documentsIdsToAccept);
	}

	@Override
	public String toString()
	{
		return "PolicyAgreementDecision [acceptanceStatus=" + acceptanceStatus + ", documentsIdsToAccept="
				+ documentsIdsToAccept + "]";
	}
}
