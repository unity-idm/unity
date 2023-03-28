/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.DBI18nString;

public class DBPolicyAgreementConfigurationTest extends DBTypeTestBase<DBPolicyAgreementConfiguration>
{

	@Override
	protected String getJson()
	{
		return "{\"documentsIdsToAccept\":[1,2],\"presentationType\":\"CHECKBOX_SELECTED\","
				+ "\"text\":{\"DefaultValue\":\"text\",\"Map\":{}}}\n";
	}

	@Override
	protected DBPolicyAgreementConfiguration getObject()
	{
		return DBPolicyAgreementConfiguration.builder()
				.withDocumentsIdsToAccept(List.of(1l, 2l))
				.withPresentationType("CHECKBOX_SELECTED")
				.withText(DBI18nString.builder()
						.withDefaultValue("text")
						.build())
				.build();
	}

}
