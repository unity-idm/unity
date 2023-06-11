/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.bulkops.BulkProcessingSupport.RuleWithTS;
import pl.edu.icm.unity.engine.utils.ScheduledUpdaterBase;
import pl.edu.icm.unity.store.api.generic.ProcessingRuleDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Checks if persisted bulk operations are changed wrt to what is loaded in QuartzScheduler
 * and if needed updates it. 
 * @author K. Benedyczak
 */
@Component
public class BulkOperationsUpdater extends ScheduledUpdaterBase
{
	@Autowired
	private ProcessingRuleDB ruleDB;
	@Autowired
	private BulkProcessingSupport bulkSupport;
	@Autowired
	private TransactionalRunner tx;
	

	public BulkOperationsUpdater()
	{
		super("bulk entity operations");
	}

	@Override
	protected void updateInternal() throws EngineException
	{
		Collection<RuleWithTS> scheduledRulesWithTS = bulkSupport.getScheduledRulesWithTS();
		
		tx.runInTransactionThrowing(() -> {
			Map<String, AbstractMap.SimpleEntry<Date, ScheduledProcessingRule>> rulesInDb = 
					getRuleChangeTime();
			Set<String> toRemove = new HashSet<>();
			Set<AbstractMap.SimpleEntry<Date, ScheduledProcessingRule>> toUpdate = new HashSet<>();
			for (RuleWithTS rule: scheduledRulesWithTS)
			{
				Map.Entry<Date, ScheduledProcessingRule> fromDb = rulesInDb.remove(rule.ruleId);
				if (fromDb == null)
					toRemove.add(rule.ruleId);
				else if (!fromDb.getKey().equals(rule.ts))
					toUpdate.add(new AbstractMap.SimpleEntry<>(fromDb.getKey(), fromDb.getValue()));
			}

			for (AbstractMap.SimpleEntry<Date, ScheduledProcessingRule> toAdd: rulesInDb.values())
				bulkSupport.scheduleJob(toAdd.getValue(), toAdd.getKey());
			for (String removed: toRemove)
				bulkSupport.undeployJob(removed);
			for (AbstractMap.SimpleEntry<Date, ScheduledProcessingRule> updated: toUpdate)
				bulkSupport.updateJob(updated.getValue(), updated.getKey());
		});
	}
	
	private Map<String, AbstractMap.SimpleEntry<Date, ScheduledProcessingRule>> getRuleChangeTime() 
			throws EngineException
	{
		Map<String, AbstractMap.SimpleEntry<Date, ScheduledProcessingRule>> changedRules = new HashMap<>();
		List<Map.Entry<ScheduledProcessingRule, Date>> ruleNames = ruleDB.getAllWithUpdateTimestamps();
		for (Map.Entry<ScheduledProcessingRule, Date> rule: ruleNames)
			changedRules.put(rule.getKey().getId(), new AbstractMap.SimpleEntry<>(
					rule.getValue(), rule.getKey()));
		return changedRules;
	}
}
