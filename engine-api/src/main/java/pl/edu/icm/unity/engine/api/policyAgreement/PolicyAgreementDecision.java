/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyAgreement;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PolicyAgreementDecision
{
	public final PolicyAgreementAcceptanceStatus acceptanceStatus;
	public final List<Long> documentsIdsToAccept;

	public PolicyAgreementDecision(PolicyAgreementAcceptanceStatus decision, List<Long> documents)
	{
		this.acceptanceStatus = decision;
		this.documentsIdsToAccept = Collections.unmodifiableList(
				documents == null ? Collections.emptyList() : documents);
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
}
