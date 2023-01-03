/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.time.Duration;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestRegistrationWrapUpConfigTest extends RestTypeBase<RestRegistrationWrapUpConfig>
{

	@Override
	protected String getJson()
	{
		return "{\"state\":\"AUTO_ACCEPTED\",\"title\":{\"DefaultValue\":\"title\",\"Map\":{}},"
				+ "\"info\":{\"DefaultValue\":\"info\",\"Map\":{}}"
				+ ",\"redirectCaption\":{\"DefaultValue\":\"redirect\""
				+ ",\"Map\":{}},\"automatic\":true,\"redirectURL\":\"redirectUrl\",\"redirectAfterTime\":86400.000000000}\n";
	}

	@Override
	protected RestRegistrationWrapUpConfig getObject()
	{
		return RestRegistrationWrapUpConfig.builder()
				.withAutomatic(true)
				.withInfo(RestI18nString.builder()
						.withDefaultValue("info")
						.build())
				.withRedirectCaption(RestI18nString.builder()
						.withDefaultValue("redirect")
						.build())
				.withTitle(RestI18nString.builder()
						.withDefaultValue("title")
						.build())
				.withRedirectAfterTime(Duration.ofDays(1))
				.withState("AUTO_ACCEPTED")
				.withRedirectURL("redirectUrl")
				.build();
	}

}
