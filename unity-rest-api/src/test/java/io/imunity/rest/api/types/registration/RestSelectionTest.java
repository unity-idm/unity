/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestSelectionTest extends RestTypeBase<RestSelection>
{

	@Override
	protected String getJson()
	{
		return "{\"selected\":true,\"externalIdp\":\"externalIdp\",\"translationProfile\":\"Profile\"}\n";
	}

	@Override
	protected RestSelection getObject()
	{
		return RestSelection.builder()
				.withExternalIdp("externalIdp")
				.withTranslationProfile("Profile")
				.withSelected(true)
				.build();
	}

}
