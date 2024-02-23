/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.store.api.generic.ProcessingRuleDB;
import pl.edu.icm.unity.base.tx.Transactional;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Implements {@link BulkProcessingManagement}.
 * @author K. Benedyczak
 */
@Component
@Primary
public class BulkProcessingManagementImpl implements BulkProcessingManagement, BulkProcessingInternal
{
	private ProcessingRuleDB db;
	private InternalAuthorizationManager authz;
	private BulkProcessingSupport bulkProcessingSupport;
	private BulkOperationsUpdater updater;

	private static final Logger log = Log.getLogger(Log.BUG_CATCHER, BulkProcessingManagementImpl.class);


	@Autowired
	public BulkProcessingManagementImpl(ProcessingRuleDB db, InternalAuthorizationManager authz,
			BulkProcessingSupport bulkProcessingSupport, BulkOperationsUpdater updater)
	{
		this.db = db;
		this.authz = authz;
		this.bulkProcessingSupport = bulkProcessingSupport;
		this.updater = updater;
	}

	@Override
	public void applyRule(TranslationRule rule) throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		bulkProcessingSupport.scheduleImmediateJob(rule);
	}
	
	@Override
	public void applyRuleSync(TranslationRule rule, long timeout) throws AuthorizationException, TimeoutException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		bulkProcessingSupport.scheduleImmediateJobSync(rule, timeout);
	}
	
	@Transactional
	@Override
	public String scheduleRule(ScheduledProcessingRuleParam rule) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		ScheduledProcessingRule fullRule = new ScheduledProcessingRule(rule.getCondition(), rule.getAction(), 
				rule.getCronExpression(), BulkProcessingSupport.generateJobKey());
		try
		{
			log.trace("Starting to save rule: {} in thread {}", fullRule, Thread.currentThread().getName());
			db.create(fullRule);
			log.trace("Saved rule {} in thread {}", db.get(fullRule.getId()), Thread.currentThread().getName());
			updater.updateManual();
		} catch (Exception e)
		{
			throw e;
		}
		return fullRule.getId();
	}

	@Transactional
	@Override
	public void removeScheduledRule(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		db.delete(id);
		updater.update();
	}

	@Transactional
	@Override
	public void removeAllRules() throws EngineException
	{
		db.deleteAll();
		updater.update();
	}
	
	@Transactional
	@Override
	public void updateScheduledRule(ScheduledProcessingRule rule) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		db.update(rule);
		updater.updateManual();
	}
	
	@Transactional
	@Override
	public List<ScheduledProcessingRule> getScheduledRules() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		log.trace("Getting all rules, in thread {}", Thread.currentThread().getName());
		return db.getAll();
	}
	
	@Transactional
	@Override
	public ScheduledProcessingRule getScheduledRule(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return db.get(id);
	}

}
