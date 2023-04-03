/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;

public class PolicyAgreementDecisionMapper
{
	public static DBPolicyAgreementDecision map(PolicyAgreementDecision policyAgreementDecision)
	{
		return DBPolicyAgreementDecision.builder()
				.withAcceptanceStatus(policyAgreementDecision.acceptanceStatus.name())
				.withDocumentsIdsToAccept(policyAgreementDecision.documentsIdsToAccept)
				.build();
	}

	public static PolicyAgreementDecision map(DBPolicyAgreementDecision restPolicyAgreementDecision)
	{
		return new PolicyAgreementDecision(
				PolicyAgreementAcceptanceStatus.valueOf(restPolicyAgreementDecision.acceptanceStatus),
				restPolicyAgreementDecision.documentsIdsToAccept);
	}
}
