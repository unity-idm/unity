/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.ProcessingRuleDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.types.translation.TranslationRule;

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
			db.create(fullRule);
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
		return db.getAll();
	}
}
