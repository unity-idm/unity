/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.time.Duration;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBRegistrationWrapUpConfigTest extends DBTypeTestBase<DBRegistrationWrapUpConfig>
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
	protected DBRegistrationWrapUpConfig getObject()
	{
		return DBRegistrationWrapUpConfig.builder()
				.withAutomatic(true)
				.withInfo(DBI18nString.builder()
						.withDefaultValue("info")
						.build())
				.withRedirectCaption(DBI18nString.builder()
						.withDefaultValue("redirect")
						.build())
				.withTitle(DBI18nString.builder()
						.withDefaultValue("title")
						.build())
				.withRedirectAfterTime(Duration.ofDays(1))
				.withState("AUTO_ACCEPTED")
				.withRedirectURL("redirectUrl")
				.build();
	}

}
