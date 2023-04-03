/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.policyDocuments;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBPolicyDocumentBaseTest extends DBTypeTestBase<DBPolicyDocumentBase>
{

	@Override
	protected String getJson()
	{
		return "{\"displayedName\":{\"DefaultValue\":\"dispName\",\"Map\":{}},"
				+ "\"revision\":1,\"mandatory\":true,\"contentType\":\"EMBEDDED\","
				+ "\"content\":{\"DefaultValue\":\"content\",\"Map\":{}}}\n";
	}

	@Override
	protected DBPolicyDocumentBase getObject()
	{
		return DBPolicyDocumentBase.builder()
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
