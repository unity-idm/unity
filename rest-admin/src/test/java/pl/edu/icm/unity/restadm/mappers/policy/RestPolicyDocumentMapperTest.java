/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.policy;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.restadm.mappers.OneWayMapperTestBase;

import java.util.Map;
import java.util.function.Function;

public class RestPolicyDocumentMapperTest extends OneWayMapperTestBase<PolicyDocumentWithRevision, RestPolicyDocument>
{
	@Override
	protected PolicyDocumentWithRevision getAPIObject()
	{
		I18nString displayedName = new I18nString();
		displayedName.addAllValues(Map.of("en", "Ola"));

		I18nString description = new I18nString();
		description.addAllValues(Map.of("en", "description"));
		return new PolicyDocumentWithRevision(
			1L,
			"Ala",
			displayedName,
			true,
			PolicyDocumentContentType.EMBEDDED,
			description,
			1
		);
	}

	@Override
	protected RestPolicyDocument getRestObject()
	{
		return RestPolicyDocument.builder()
			.withId(1L)
			.withName("Ala")
			.withDisplayedName(Map.of("en", "Ola"))
			.withMandatory(true)
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withRevision(1)
			.withContent(Map.of("en", "description"))
			.build();
	}

	@Override
	protected Function<PolicyDocumentWithRevision, RestPolicyDocument> getMapper()
	{
		return PolicyDocumentMapper::map;
	}
}
