/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.cred;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBCredentialDefinitionTest extends DBTypeTestBase<DBCredentialDefinition>
{

	@Override
	protected String getJson()
	{
		return "{\"typeId\":\"type\",\"name\":\"name\",\"readOnly\":true,\"configuration\":\"config\","
				+ "\"displayedName\":{\"DefaultValue\":\"disp\",\"Map\":{}},\"i18nDescription\":{\"DefaultValue\":\"desc\","
				+ "\"Map\":{}}}\n";
	}

	@Override
	protected DBCredentialDefinition getObject()
	{
		return DBCredentialDefinition.builder()
				.withConfiguration("config")
				.withI18nDescription(DBI18nString.builder()
						.withDefaultValue("desc")
						.build())
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("disp")
						.build())
				.withName("name")
				.withTypeId("type")
				.withReadOnly(true)
				.build();
	}

}
