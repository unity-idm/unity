/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;



public class PolicyAgreementDecisionMapperTest
		extends MapperTestBase<PolicyAgreementDecision, DBPolicyAgreementDecision>
{

	@Override
	protected PolicyAgreementDecision getFullAPIObject()
	{
		return new PolicyAgreementDecision(PolicyAgreementAcceptanceStatus.ACCEPTED, List.of(1L, 2L));
	}

	@Override
	protected DBPolicyAgreementDecision getFullDBObject()
	{
		return DBPolicyAgreementDecision.builder()
				.withAcceptanceStatus("ACCEPTED")
				.withDocumentsIdsToAccept(List.of(1L, 2L))
				.build();
	}

	@Override
	protected Pair<Function<PolicyAgreementDecision, DBPolicyAgreementDecision>, Function<DBPolicyAgreementDecision, PolicyAgreementDecision>> getMapper()
	{
		return Pair.of(PolicyAgreementDecisionMapper::map, PolicyAgreementDecisionMapper::map);
	}

}
