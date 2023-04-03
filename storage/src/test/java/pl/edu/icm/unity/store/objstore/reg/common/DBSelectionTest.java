/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBSelectionTest extends DBTypeTestBase<DBSelection>
{

	@Override
	protected String getJson()
	{
		return "{\"selected\":true,\"externalIdp\":\"externalIdp\",\"translationProfile\":\"Profile\"}\n";
	}

	@Override
	protected DBSelection getObject()
	{
		return DBSelection.builder()
				.withExternalIdp("externalIdp")
				.withTranslationProfile("Profile")
				.withSelected(true)
				.build();
	}

}
