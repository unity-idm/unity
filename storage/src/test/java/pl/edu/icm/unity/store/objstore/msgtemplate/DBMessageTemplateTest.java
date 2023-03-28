/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.msgtemplate;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.DBI18nString;

public class DBMessageTemplateTest extends DBTypeTestBase<DBMessageTemplate>
{

	@Override
	protected String getJson()
	{
		return "{\"name\":\"name\",\"description\":\"desc\",\"consumer\":\"consumer\",\"type\":\"PLAIN\","
				+ "\"messages\":[{\"locale\":\"\",\"subject\":\"sub\",\"body\":\"body\"}],"
				+ "\"notificationChannel\":\"channel\"}\n";
	}

	@Override
	protected DBMessageTemplate getObject()
	{
		return DBMessageTemplate.builder()
				.withName("name")
				.withConsumer("consumer")
				.withType("PLAIN")
				.withDescription("desc")
				.withNotificationChannel("channel")
				.withMessage(DBI18nMessage.builder()
						.withBody(DBI18nString.builder()
								.withDefaultValue("body")
								.build())
						.withSubject(DBI18nString.builder()
								.withDefaultValue("sub")
								.build())
						.build())
				.build();
	}

}
