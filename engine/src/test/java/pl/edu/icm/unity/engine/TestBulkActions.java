/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.function.Supplier;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.server.api.BulkProcessingManagement;
import pl.edu.icm.unity.server.bulkops.EntityAction;
import pl.edu.icm.unity.server.bulkops.ProcessingRule;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.server.bulkops.action.ChangeStatusActionFactory;
import pl.edu.icm.unity.server.bulkops.action.RemoveEntityActionFactory;
import pl.edu.icm.unity.server.translation.form.action.SetEntityStateActionFactory.EntityStateLimited;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestBulkActions extends DBIntegrationTestBase
{
	@Autowired
	private BulkProcessingManagement bulkMan;
	
	@Autowired
	private ChangeStatusActionFactory changeEntityActionFactory;

	@Autowired
	private RemoveEntityActionFactory removeEntityActionFactory;
	
	@Test
	public void immediateRuleIsExecuted() throws Exception
	{
		IdentityParam testUser = new IdentityParam(UsernameIdentity.ID, "test-user");
		idsMan.addEntity(testUser, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);

		ProcessingRule rule = new ProcessingRule("idsByType['userName'] contains 'test-user' and "
				+ "status == 'valid' and "
				+ "credReq == '" + EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT + "'", 
				changeEntityActionFactory.getInstance(
						EntityStateLimited.disabled.toString()));
		
		bulkMan.applyRule(rule);

		waitFor(300, 1000, 7, () -> {
			Entity entity;
			try
			{
				entity = idsMan.getEntity(new EntityParam(testUser));
			} catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}
			return entity.getState() == EntityState.disabled;
		});
	}
	
	private static void waitFor(long initWait, long delay, int maxTries, Supplier<Boolean> supplier) 
			throws Exception
	{
		Thread.sleep(initWait);
		for (int i=0; i<maxTries; i++)
		{
			if (supplier.get())
				return;
			Thread.sleep(delay);
		}
		fail("Maximum wait time exceeded, the requested result was not obtained");
	}
	
	@Test
	public void scheduledRuleIsExecuted() throws Exception
	{
		IdentityParam testUser = new IdentityParam(UsernameIdentity.ID, "test-user");
		idsMan.addEntity(testUser, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid, false);
		
		ScheduledProcessingRuleParam rule = new ScheduledProcessingRuleParam("idsByType['userName'] contains 'test-user' and "
				+ "status == 'valid' and "
				+ "credReq == '" + EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT + "'", 
				changeEntityActionFactory.getInstance(EntityStateLimited.disabled.toString()),
				"0/2 * * * * ?");
		
		bulkMan.scheduleRule(rule);

		waitFor(300, 1000, 7, () -> {
			Entity entity;
			try
			{
				entity = idsMan.getEntity(new EntityParam(testUser));
			} catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}
			return entity.getState() == EntityState.disabled;
		});
	}

	@Test
	public void existingRuleCanBeRemoved() throws Exception
	{
		ScheduledProcessingRuleParam rule = createTestRule();
		String id = bulkMan.scheduleRule(rule);
		
		bulkMan.removeScheduledRule(id);
		
		assertThat(bulkMan.getScheduledRules().isEmpty(), is(true));
	}

	@Test
	public void existingRuleCanBeUpdated() throws Exception
	{
		ScheduledProcessingRuleParam rule = createTestRule();
		String id = bulkMan.scheduleRule(rule);
		
		ScheduledProcessingRule rule2 = new ScheduledProcessingRule("false", 
				changeEntityActionFactory.getInstance(
						EntityStateLimited.disabled.toString()), 
				"20 10 10 * * ?", id);
		bulkMan.updateScheduledRule(rule2);
		
		Collection<ScheduledProcessingRule> scheduledRules = bulkMan.getScheduledRules();
		assertThat(scheduledRules.size(), is(1));
		ScheduledProcessingRule rulePrim = scheduledRules.iterator().next();
		assertThat(rulePrim, is(rule2));		
	}
	
	@Test
	public void scheduledRulesAreListed() throws Exception
	{
		ScheduledProcessingRuleParam rule = createTestRule();
		String id = bulkMan.scheduleRule(rule);
		
		Collection<ScheduledProcessingRule> scheduledRules = bulkMan.getScheduledRules();
		assertThat(scheduledRules.size(), is(1));
		ScheduledProcessingRule rulePrim = scheduledRules.iterator().next();
		assertThat(rulePrim.getAction(), is(rule.getAction()));
		assertThat(rulePrim.getCondition(), is(rule.getCondition()));
		assertThat(rulePrim.getCronExpression(), is(rule.getCronExpression()));
		assertThat(rulePrim.getId(), is(id));
	}

	
	private ScheduledProcessingRuleParam createTestRule()
	{
		EntityAction action = removeEntityActionFactory.getInstance();
		return new ScheduledProcessingRuleParam("true", action, "10 10 10 * * ?");
	}
}
