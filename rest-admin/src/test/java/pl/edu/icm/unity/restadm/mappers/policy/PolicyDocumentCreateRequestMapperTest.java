/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.policy;

import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.restadm.mappers.OneWayMapperTestBase;

import java.util.Map;
import java.util.function.Function;

public class PolicyDocumentCreateRequestMapperTest extends OneWayMapperTestBase<RestPolicyDocumentRequest, PolicyDocumentCreateRequest>
{
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
	protected PolicyDocumentCreateRequest getRestObject()
	{
		return PolicyDocumentCreateRequest.createRequestBuilder()
			.withName("Ala")
			.withDisplayedName(Map.of("en", "Ola"))
			.withMandatory(true)
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
	}

	@Override
	protected Function<RestPolicyDocumentRequest, PolicyDocumentCreateRequest> getMapper()
	{
		return PolicyDocumentMapper::map;
	}
}
