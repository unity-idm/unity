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

import org.apache.ibatis.session.SqlSession;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.bulk.ProcessingRuleDB;
import pl.edu.icm.unity.engine.bulkops.BulkProcessingSupport.RuleWithTS;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.utils.ScheduledUpdaterBase;

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
	private Scheduler scheduler;
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
		Collection<RuleWithTS> scheduledRulesWithTS;
		scheduledRulesWithTS = bulkSupport.getScheduledRulesWithTS();
		
		tx.runInTransaction(() -> {
			SqlSession sql = SqlSessionTL.get();
			Map<String, AbstractMap.SimpleEntry<Date, ScheduledProcessingRule>> rulesInDb = 
					getRuleChangeTime(sql);
			Set<String> toRemove = new HashSet<>();
			Set<AbstractMap.SimpleEntry<Date, ScheduledProcessingRule>> toUpdate = new HashSet<>();
			for (RuleWithTS rule: scheduledRulesWithTS)
			{
				Map.Entry<Date, ScheduledProcessingRule> fromDb = rulesInDb.remove(rule.rule.getId());
				if (fromDb == null)
					toRemove.add(rule.rule.getId());
				else if (!fromDb.getKey().equals(rule.ts))
					toUpdate.add(new AbstractMap.SimpleEntry<>(rule.ts, rule.rule));
			}

			for (AbstractMap.SimpleEntry<Date, ScheduledProcessingRule> toAdd: rulesInDb.values())
				bulkSupport.scheduleJob(toAdd.getValue(), toAdd.getKey());
			for (String removed: toRemove)
				bulkSupport.undeployJob(removed);
			for (AbstractMap.SimpleEntry<Date, ScheduledProcessingRule> updated: toUpdate)
				bulkSupport.updateJob(updated.getValue(), updated.getKey());
		});
	}
	
	private Map<String, AbstractMap.SimpleEntry<Date, ScheduledProcessingRule>> getRuleChangeTime(SqlSession sql) 
			throws EngineException
	{
		Map<String, AbstractMap.SimpleEntry<Date, ScheduledProcessingRule>> changedRules = new HashMap<>();
		List<Map.Entry<ScheduledProcessingRule, Date>> ruleNames = ruleDB.getAllWithUpdateTimestamps(sql);
		for (Map.Entry<ScheduledProcessingRule, Date> rule: ruleNames)
			changedRules.put(rule.getKey().getId(), new AbstractMap.SimpleEntry<>(
					rule.getValue(), rule.getKey()));
		return changedRules;
	}
}
