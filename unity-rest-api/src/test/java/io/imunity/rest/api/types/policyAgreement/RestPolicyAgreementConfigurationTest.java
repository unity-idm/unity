/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.policyAgreement;

import java.util.List;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestPolicyAgreementConfigurationTest extends RestTypeBase<RestPolicyAgreementConfiguration>
{

	@Override
	protected String getJson()
	{
		return "{\"documentsIdsToAccept\":[1,2],\"presentationType\":\"CHECKBOX_SELECTED\","
				+ "\"text\":{\"DefaultValue\":\"text\",\"Map\":{}}}\n";
	}

	@Override
	protected RestPolicyAgreementConfiguration getObject()
	{
		return RestPolicyAgreementConfiguration.builder()
				.withDocumentsIdsToAccept(List.of(1l, 2l))
				.withPresentationType("CHECKBOX_SELECTED")
				.withText(RestI18nString.builder()
						.withDefaultValue("text")
						.build())
				.build();
	}

}
