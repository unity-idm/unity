/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBGroupSelectionTest extends DBTypeTestBase<DBGroupSelection>
{

	@Override
	protected String getJson()
	{
		return "{\"selectedGroups\":[\"/g1\",\"/g2\"],\"externalIdp\":\"externalIdp\","
				+ "\"translationProfile\":\"Profile\"}\n";
	}

	@Override
	protected DBGroupSelection getObject()
	{
		return DBGroupSelection.builder()
				.withExternalIdp("externalIdp")
				.withTranslationProfile("Profile")
				.withSelectedGroups(List.of("/g1", "/g2"))
				.build();
	}

}
