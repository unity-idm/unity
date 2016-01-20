/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.bulk.ProcessingRuleDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.bulkops.BulkOperationsUpdater;
import pl.edu.icm.unity.engine.bulkops.BulkProcessingSupport;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.BulkProcessingManagement;
import pl.edu.icm.unity.server.bulkops.ProcessingRule;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRuleParam;

/**
 * Implements {@link BulkProcessingManagement}.
 * @author K. Benedyczak
 */
@Component
public class BulkProcessingManagementImpl implements BulkProcessingManagement
{
	private ProcessingRuleDB db;
	private AuthorizationManager authz;
	private BulkProcessingSupport bulkProcessingSupport;
	private BulkOperationsUpdater updater;

	@Autowired
	public BulkProcessingManagementImpl(ProcessingRuleDB db, AuthorizationManager authz,
			BulkProcessingSupport bulkProcessingSupport, BulkOperationsUpdater updater)
	{
		this.db = db;
		this.authz = authz;
		this.bulkProcessingSupport = bulkProcessingSupport;
		this.updater = updater;
	}

	@Override
	public void applyRule(ProcessingRule rule) throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		bulkProcessingSupport.scheduleImmediateJob(rule);
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
			db.insert(fullRule.getId(), fullRule, SqlSessionTL.get());
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
		db.remove(id, SqlSessionTL.get());
		updater.update();
	}

	@Transactional
	@Override
	public void updateScheduledRule(ScheduledProcessingRule rule) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		db.update(rule.getId(), rule, SqlSessionTL.get());
		updater.updateManual();
	}
	
	@Transactional
	@Override
	public List<ScheduledProcessingRule> getScheduledRules() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return db.getAll(SqlSessionTL.get());
	}
}
