/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.policy;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;

public class PolicyDocumentMapper
{
	public static RestPolicyDocument map(PolicyDocumentWithRevision policyDocument)
	{
		return RestPolicyDocument.builder()
			.withId(policyDocument.id)
			.withName(policyDocument.name)
			.withDisplayedName(policyDocument.displayedName.getMap())
			.withMandatory(policyDocument.mandatory)
			.withContentType(policyDocument.contentType.name())
			.withRevision(policyDocument.revision)
			.withContent(policyDocument.content.getMap())
			.build();
	}

	public static PolicyDocumentCreateRequest map(RestPolicyDocumentRequest policyDocument)
	{
		return PolicyDocumentCreateRequest.createRequestBuilder()
			.withName(policyDocument.name)
			.withDisplayedName(policyDocument.displayedName)
			.withMandatory(policyDocument.mandatory)
			.withContentType(policyDocument.contentType)
			.withContent(policyDocument.content)
			.build();
	}

	public static PolicyDocumentUpdateRequest map(long id, RestPolicyDocumentRequest policyDocument)
	{
		return PolicyDocumentUpdateRequest.updateRequestBuilder()
			.withId(id)
			.withName(policyDocument.name)
			.withDisplayedName(policyDocument.displayedName)
			.withMandatory(policyDocument.mandatory)
			.withContentType(policyDocument.contentType)
			.withContent(policyDocument.content)
			.build();
	}
}
