/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.tprofile;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBTranslationRuleTest extends DBTypeTestBase<DBTranslationRule>
{

	@Override
	protected String getJson()
	{
		return "{\"condition\":\"true\",\"action\":{\"name\":\"action\",\"parameters\":[\"p1\",\"p2\"]}}";
	}

	@Override
	protected DBTranslationRule getObject()
	{
		return DBTranslationRule.builder()
				.withCondition("true")
				.withAction(DBTranslationAction.builder()
						.withName("action")
						.withParameters(List.of("p1", "p2"))
						.build())
				.build();
	}

}
