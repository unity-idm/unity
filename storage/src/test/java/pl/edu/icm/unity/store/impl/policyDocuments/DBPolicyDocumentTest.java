/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.policyDocuments;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBPolicyDocumentTest extends DBTypeTestBase<DBPolicyDocument>
{

	@Override
	protected String getJson()
	{
		return "{\"id\":1,\"name\":\"name\",\"displayedName\":{\"DefaultValue\":\"dispName\",\"Map\":{}},"
				+ "\"revision\":1,\"mandatory\":true,\"contentType\":\"EMBEDDED\","
				+ "\"content\":{\"DefaultValue\":\"content\",\"Map\":{}}}\n";
	}

	@Override
	protected DBPolicyDocument getObject()
	{
		return DBPolicyDocument.builder()
				.withId(1L)
				.withName("name")
				.withContent(DBI18nString.builder()
						.withDefaultValue("content")
						.build())
				.withContentType("EMBEDDED")
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("dispName")
						.build())
				.withMandatory(true)
				.withRevision(1)
				.build();
	}

}
