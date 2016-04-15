/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;
import java.util.concurrent.TimeoutException;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
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
	 * @throws AuthorizationException 
	 */
	void applyRule(ProcessingRule rule) throws AuthorizationException;
	
	String scheduleRule(ScheduledProcessingRuleParam rule) throws EngineException;
	
	void removeScheduledRule(String id) throws EngineException;
	
	void updateScheduledRule(ScheduledProcessingRule rule) throws EngineException;
	
	List<ScheduledProcessingRule> getScheduledRules() throws EngineException;

	void applyRuleSync(ProcessingRule rule, long timeout)
			throws AuthorizationException, TimeoutException;
}
