/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.tprofile;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile.Condition;
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile.DBTranslationProfileRule;

public class DBTranslationProfileTest extends DBTypeTestBase<DBTranslationProfile>
{

	@Override
	protected String getJson()
	{
		return "{\"ver\":\"2\",\"name\":\"name\",\"description\":\"desc\",\"type\":\"REGISTRATION\",\"mode\":\"DEFAULT\","
				+ "\"rules\":[{\"condition\":{\"conditionValue\":\"true\"},\"action\":{\"name\":\"action\","
				+ "\"parameters\":[\"p1\",\"p2\"]}}]}\n";
	}

	@Override
	protected DBTranslationProfile getObject()
	{
		return DBTranslationProfile.builder()
				.withDescription("desc")
				.withName("name")
				.withMode("DEFAULT")
				.withType("REGISTRATION")
				.withRules(List.of(DBTranslationProfileRule.builder()
						.withCondition(Condition.builder().withConditionValue("true").build())
						.withAction(DBTranslationAction.builder()
								.withName("action")
								.withParameters(List.of("p1", "p2"))
								.build())
						.build()))
				.build();
	}

}
