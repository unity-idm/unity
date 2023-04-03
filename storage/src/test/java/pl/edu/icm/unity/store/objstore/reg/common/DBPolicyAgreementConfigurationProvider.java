/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.List;

import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBPolicyAgreementConfigurationProvider
{
	public static DBPolicyAgreementConfiguration getParam()
	{
		return DBPolicyAgreementConfiguration.builder()
		.withDocumentsIdsToAccept(List.of(1L))
		.withPresentationType("CHECKBOX_SELECTED")
		.withText(DBI18nString.builder()
				.withDefaultValue("text")
				.build())
		.build();
	}
}
