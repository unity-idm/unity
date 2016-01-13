/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Collection;

import pl.edu.icm.unity.server.bulkops.ProcessingRule;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRuleParam;

/**
 * Defines API allowing for bulk processing of entities - both manually and basing on a repeatable schedule.
 * @author K. Benedyczak
 */
public interface BulkProcessingManagement
{
	/**
	 * Schedule a rule to be invoked immediately.
	 * @param rule
	 */
	void applyRule(ProcessingRule rule);
	
	void scheduleRule(ScheduledProcessingRuleParam rule);
	
	void removeScheduledRule(String id);
	
	void updateScheduledRule(ScheduledProcessingRule rule);
	
	Collection<ScheduledProcessingRule> getScheduledRules();
}
