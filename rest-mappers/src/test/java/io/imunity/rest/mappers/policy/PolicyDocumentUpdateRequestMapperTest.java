/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.policy;

import java.util.Map;
import java.util.function.Function;

import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.mappers.OneWayMapperTestBase;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;

public class PolicyDocumentUpdateRequestMapperTest extends OneWayMapperTestBase<RestPolicyDocumentRequest, PolicyDocumentUpdateRequest>
{
	private final static long ID = 1L;

	@Override
	protected RestPolicyDocumentRequest getAPIObject()
	{
		return RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
	}

	@Override
	protected PolicyDocumentUpdateRequest getRestObject()
	{
		return PolicyDocumentUpdateRequest.updateRequestBuilder()
			.withId(ID)
			.withName("Ala")
			.withDisplayedName(Map.of("en", "Ola"))
			.withMandatory(true)
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
	}

	@Override
	protected Function<RestPolicyDocumentRequest, PolicyDocumentUpdateRequest> getMapper()
	{
		return policyDocument -> PolicyDocumentMapper.map(ID, policyDocument);
	}
}
