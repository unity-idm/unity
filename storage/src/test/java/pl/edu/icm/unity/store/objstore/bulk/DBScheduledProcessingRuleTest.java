/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.bulk;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBScheduledProcessingRuleTest extends DBTypeTestBase<DBScheduledProcessingRule>
{
	@Override
	protected String getJson()
	{
		return "{\"cronExpression\":\"cron\",\"condition\":\"cond\",\"action\":\"action\",\"actionParams\":[\"p1\",\"p2\"],\"id\":\"id\"}\n";
	}

	@Override
	protected DBScheduledProcessingRule getObject()
	{
		return DBScheduledProcessingRule.builder()
				.withCronExpression("cron")
				.withAction("action")
				.withActionParams(List.of("p1", "p2"))
				.withCondition("cond")
				.withId("id")
				.build();
	}

}
