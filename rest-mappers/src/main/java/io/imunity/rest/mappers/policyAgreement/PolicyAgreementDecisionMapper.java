/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.policyAgreement;

import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementDecision;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;

public class PolicyAgreementDecisionMapper
{
	public static RestPolicyAgreementDecision map(PolicyAgreementDecision policyAgreementDecision)
	{
		return RestPolicyAgreementDecision.builder()
				.withAcceptanceStatus(policyAgreementDecision.acceptanceStatus.name())
				.withDocumentsIdsToAccept(policyAgreementDecision.documentsIdsToAccept)
				.build();
	}

	public static PolicyAgreementDecision map(RestPolicyAgreementDecision restPolicyAgreementDecision)
	{
		return new PolicyAgreementDecision(
				PolicyAgreementAcceptanceStatus.valueOf(restPolicyAgreementDecision.acceptanceStatus),
				restPolicyAgreementDecision.documentsIdsToAccept);
	}
}
