/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.core.QuartzScheduler;
import org.quartz.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.bulk.ProcessingRuleDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.BulkProcessingExecutor;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.BulkProcessingManagement;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
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
	private static final String RULE_KEY = "rule";
	private static final String JOB_GROUP = "bulkEntityProcessing";
	
	@Autowired
	private QuartzScheduler scheduler;
	@Autowired
	private ProcessingRuleDB db;
	@Autowired
	private AuthorizationManager authz;
	@Autowired
	private TransactionalRunner tx;
	@Autowired
	private BulkProcessingExecutor executor;
	
	@Override
	public void applyRule(ProcessingRule rule)
	{
		JobDetail job = createJob(rule);

		Trigger trigger = TriggerBuilder.newTrigger()
				.startNow()
				.withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.build();

		try
		{
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException e)
		{
			throw new InternalException("Can not schedule rule execution", e);
		}
	}
	
	@Transactional
	@Override
	public void scheduleRule(ScheduledProcessingRuleParam rule) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		JobDetail job = createJob(rule);
		Trigger trigger = createCronTrigger(rule);
		
		ScheduledProcessingRule fullRule = new ScheduledProcessingRule(rule.getCondition(), rule.getAction(), 
				rule.getCronExpression(), job.getKey().getName());
		db.insert(fullRule.getId(), fullRule, SqlSessionTL.get());
		
		scheduleJob(job, trigger);
	}

	@Transactional
	@Override
	public void removeScheduledRule(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		undeployJob(id);
		db.remove(id, SqlSessionTL.get());
	}

	@Transactional
	@Override
	public void updateScheduledRule(ScheduledProcessingRule rule) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		undeployJob(rule.getId());
		JobDetail job = createJob(rule);
		Trigger trigger = createCronTrigger(rule);
		db.update(rule.getId(), rule, SqlSessionTL.get());
		
		scheduleJob(job, trigger);
	}

	@Override
	public Collection<ScheduledProcessingRule> getScheduledRules() throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		List<JobExecutionContext> jobs = scheduler.getCurrentlyExecutingJobs();
		return jobs.stream().
			filter(context -> context.getJobDetail().getKey().getGroup().equals(JOB_GROUP) && 
					context.get(RULE_KEY) instanceof ScheduledProcessingRule).
			map(context -> {
				ScheduledProcessingRule rule = (ScheduledProcessingRule) context.get(RULE_KEY);
				return new ScheduledProcessingRule(rule.getCondition(), rule.getAction(), 
					rule.getCronExpression(), rule.getId());	
			}).
			collect(Collectors.toList());
	}

	private JobDetail createJob(ProcessingRule rule)
	{
		JobDataMap dataMap = new JobDataMap();
		dataMap.put(RULE_KEY, rule);
		JobDetail job = JobBuilder.newJob(EntityRuleJob.class)
				.withIdentity(Key.createUniqueName(null), JOB_GROUP)
				.usingJobData(dataMap)
				.build();
		return job;
	}
	
	private Trigger createCronTrigger(ScheduledProcessingRuleParam rule)
	{
		return TriggerBuilder.newTrigger()
				.withSchedule(CronScheduleBuilder.cronSchedule(rule.getCronExpression()))
				.build();
	}
	
	private void scheduleJob(JobDetail job, Trigger trigger)
	{
		try
		{
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException e)
		{
			throw new InternalException("Can't schedule processing rule", e);
		}
	}
	
	private void undeployJob(String id)
	{
		try
		{
			scheduler.deleteJob(new JobKey(id, JOB_GROUP));
		} catch (SchedulerException e)
		{
			throw new InternalException("Can't undeploy a rule with id " + id, e);
		}
	}
	
	private class EntityRuleJob implements Job 
	{
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException
		{
			ProcessingRule rule = (ProcessingRule) context.get(RULE_KEY);
			executor.execute(rule);
		}
	}
}
