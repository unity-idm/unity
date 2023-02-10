/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.policyAgreement;

import java.util.List;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestPolicyAgreementDecisionTest extends RestTypeBase<RestPolicyAgreementDecision>
{
	@Override
	protected String getJson()
	{
		return "{\"documentsIdsToAccept\":[1,2],\"acceptanceStatus\":\"ACCEPTED\"}\n";
	}

	@Override
	protected RestPolicyAgreementDecision getObject()
	{
		return RestPolicyAgreementDecision.builder()
				.withAcceptanceStatus("ACCEPTED")
				.withDocumentsIdsToAccept(List.of(1L, 2L))
				.build();
	}

}
