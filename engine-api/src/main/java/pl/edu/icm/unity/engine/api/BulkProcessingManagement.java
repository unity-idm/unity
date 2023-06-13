/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;
import java.util.concurrent.TimeoutException;

import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;

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
	void applyRule(TranslationRule rule) throws AuthorizationException;

	void applyRuleSync(TranslationRule rule, long timeout)
			throws AuthorizationException, TimeoutException;
	
	String scheduleRule(ScheduledProcessingRuleParam rule) throws EngineException;
	
	void removeScheduledRule(String id) throws EngineException;
	
	void updateScheduledRule(ScheduledProcessingRule rule) throws EngineException;
	
	List<ScheduledProcessingRule> getScheduledRules() throws EngineException;
	
	ScheduledProcessingRule getScheduledRule(String id) throws EngineException;
}
