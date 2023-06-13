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

import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.EntityState;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.bulkops.action.ChangeStatusActionFactory;
import pl.edu.icm.unity.engine.bulkops.action.RemoveEntityActionFactory;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.SetEntityStateActionFactory.EntityStateLimited;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class TestBulkActions extends DBIntegrationTestBase
{
	@Autowired
	private BulkProcessingManagement bulkMan;
	
	@Test
	public void immediateRuleIsExecuted() throws Exception
	{
		IdentityParam testUser = new IdentityParam(UsernameIdentity.ID, "test-user");
		idsMan.addEntity(testUser, EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				EntityState.valid);

		TranslationRule rule = new TranslationRule("idsByType['userName'] contains 'test-user' and "
				+ "status == 'valid' and "
				+ "credReq == '" + EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT + "'", 
				new TranslationAction(ChangeStatusActionFactory.NAME, 
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
				EntityState.valid);
		
		ScheduledProcessingRuleParam rule = new ScheduledProcessingRuleParam("idsByType['userName'] contains 'test-user' and "
				+ "status == 'valid' and "
				+ "credReq == '" + EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT + "'", 
				new TranslationAction(ChangeStatusActionFactory.NAME, 
						EntityStateLimited.disabled.toString()),
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
				new TranslationAction(ChangeStatusActionFactory.NAME, 
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
		TranslationAction taction = new TranslationAction(RemoveEntityActionFactory.NAME);
		return new ScheduledProcessingRuleParam("true", taction, "10 10 10 * * ?");
	}
}
