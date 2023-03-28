/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.bulk;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.types.translation.TranslationAction;

public class ScheduledProcessingRuleMapperTest
		extends MapperTestBase<ScheduledProcessingRule, DBScheduledProcessingRule>
{

	@Override
	protected ScheduledProcessingRule getFullAPIObject()
	{
		TranslationAction translationAction = new TranslationAction("action", "p1", "p2");
		return new ScheduledProcessingRule("cond", translationAction, "cron", "id");
	}

	@Override
	protected DBScheduledProcessingRule getFullDBObject()
	{
		return DBScheduledProcessingRule.builder()
				.withCronExpression("cron")
				.withAction("action")
				.withActionParams(List.of("p1", "p2"))
				.withCondition("cond")
				.withId("id")
				.build();
	}

	@Override
	protected Pair<Function<ScheduledProcessingRule, DBScheduledProcessingRule>, Function<DBScheduledProcessingRule, ScheduledProcessingRule>> getMapper()
	{
		return Pair.of(ScheduledProcessingRuleMapper::map, ScheduledProcessingRuleMapper::map);
	}

}
