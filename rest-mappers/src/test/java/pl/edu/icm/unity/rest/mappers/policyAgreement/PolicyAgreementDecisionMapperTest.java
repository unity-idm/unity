/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.policyAgreement;

import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementDecision;
import pl.edu.icm.unity.rest.mappers.MapperTestBase;
import pl.edu.icm.unity.rest.mappers.Pair;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;

public class PolicyAgreementDecisionMapperTest
		extends MapperTestBase<PolicyAgreementDecision, RestPolicyAgreementDecision>
{

	@Override
	protected PolicyAgreementDecision getFullAPIObject()
	{
		return new PolicyAgreementDecision(PolicyAgreementAcceptanceStatus.ACCEPTED, List.of(1L, 2L));
	}

	@Override
	protected RestPolicyAgreementDecision getFullRestObject()
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
