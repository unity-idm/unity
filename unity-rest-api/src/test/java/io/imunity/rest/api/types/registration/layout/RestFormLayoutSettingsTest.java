/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.layout;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestFormLayoutSettingsTest extends RestTypeBase<RestFormLayoutSettings>
{

	@Override
	protected String getJson()
	{
		return "{\"compactInputs\":true,\"showCancel\":true,\"columnWidth\":10.0,\"columnWidthUnit\":\"EM\","
				+ "\"logoURL\":\"logo\"}\n";
	}

	@Override
	protected RestFormLayoutSettings getObject()
	{
		return RestFormLayoutSettings.builder()
				.withCompactInputs(true)
				.withShowCancel(true)
				.withColumnWidth(10)
				.withColumnWidthUnit("EM")
				.withLogoURL("logo")
				.build();
	}

}
