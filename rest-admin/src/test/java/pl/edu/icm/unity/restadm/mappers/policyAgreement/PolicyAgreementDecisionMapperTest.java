/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.policyAgreement;

import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementDecision;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

public class PolicyAgreementDecisionMapperTest
		extends MapperTestBase<PolicyAgreementDecision, RestPolicyAgreementDecision>
{

	@Override
	protected PolicyAgreementDecision getAPIObject()
	{
		return new PolicyAgreementDecision(PolicyAgreementAcceptanceStatus.ACCEPTED, List.of(1L, 2L));
	}

	@Override
	protected RestPolicyAgreementDecision getRestObject()
	{
		return RestPolicyAgreementDecision.builder()
				.withAcceptanceStatus("ACCEPTED")
				.withDocumentsIdsToAccept(List.of(1L, 2L))
				.build();
	}

	@Override
	protected Pair<Function<PolicyAgreementDecision, RestPolicyAgreementDecision>, Function<RestPolicyAgreementDecision, PolicyAgreementDecision>> getMapper()
	{
		return Pair.of(PolicyAgreementDecisionMapper::map, PolicyAgreementDecisionMapper::map);
	}

}
