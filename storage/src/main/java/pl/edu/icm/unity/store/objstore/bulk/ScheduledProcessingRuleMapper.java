/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.bulk;

import java.util.List;

import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.translation.TranslationAction;

class ScheduledProcessingRuleMapper
{
	static DBScheduledProcessingRule map(ScheduledProcessingRule scheduledProcessingRule)
	{
		return DBScheduledProcessingRule.builder()
				.withAction(scheduledProcessingRule.getAction()
						.getName())
				.withActionParams(List.of(scheduledProcessingRule.getAction()
						.getParameters()))
				.withCondition(scheduledProcessingRule.getCondition())
				.withCronExpression(scheduledProcessingRule.getCronExpression())
				.withId(scheduledProcessingRule.getId())
				.build();
	}

	static ScheduledProcessingRule map(DBScheduledProcessingRule dbScheduledProcessingRule)
	{
		return new ScheduledProcessingRule(dbScheduledProcessingRule.condition,
				new TranslationAction(dbScheduledProcessingRule.action,
						dbScheduledProcessingRule.actionParams.toArray(String[]::new)),
				dbScheduledProcessingRule.cronExpression, dbScheduledProcessingRule.id);
	}

}
