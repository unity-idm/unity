/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.List;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestGroupSelectionTest extends RestTypeBase<RestGroupSelection>
{

	@Override
	protected String getJson()
	{
		return "{\"selectedGroups\":[\"/g1\",\"/g2\"],\"externalIdp\":\"externalIdp\","
				+ "\"translationProfile\":\"Profile\"}\n";
	}

	@Override
	protected RestGroupSelection getObject()
	{
		return RestGroupSelection.builder()
				.withExternalIdp("externalIdp")
				.withTranslationProfile("Profile")
				.withSelectedGroups(List.of("/g1", "/g2"))
				.build();
	}

}
