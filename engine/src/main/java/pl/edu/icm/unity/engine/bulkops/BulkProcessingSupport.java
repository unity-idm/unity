/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.engine.api.bulkops.EntityActionFactory;
import pl.edu.icm.unity.engine.api.bulkops.EntityActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * Utility code for bulk processing actions management
 * @author K. Benedyczak
 */
@Component
public class BulkProcessingSupport
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, BulkProcessingSupport.class);
	public static final String RULE_KEY = "rule";
	public static final String EXECUTOR_KEY = "executor";
	public static final String TS_KEY = "timeStamp";
	public static final String JOB_GROUP = "bulkEntityProcessing";
	
	@Autowired
	private Scheduler scheduler;
	@Autowired
	private BulkProcessingExecutor executor;
	@Autowired
	private EntityActionsRegistry actionsRegistry;
	
	public synchronized Collection<RuleWithTS> getScheduledRulesWithTS()
	{
		Set<JobKey> jobs = getCurrentJobs();
		return jobs.stream().
			map(this::getJobDetail).
			filter(this::filterProcessingJobs).
			map(job -> {
				Date ts = (Date) job.getJobDataMap().get(TS_KEY);
				return new RuleWithTS(job.getKey().getName(), ts);
			}).
			collect(Collectors.toList());
	}

	private Set<JobKey> getCurrentJobs()
	{
		try
		{
			return scheduler.getJobKeys(GroupMatcher.jobGroupEquals(JOB_GROUP));
		} catch (SchedulerException e)
		{
			throw new InternalException("Error retrieving scheduled jobs from Quartz", e);
		}
	}
	
	public static String generateJobKey()
	{
		return Key.createUniqueName(null);
	}

	public void scheduleImmediateJob(TranslationRule rule)
	{
		Trigger trigger = createImmediateTrigger();
		scheduleJob(createRuleInstance(rule), trigger, generateJobKey(), new Date());
	}

	public void scheduleImmediateJobSync(TranslationRule rule, long maxWaitTimeS) throws TimeoutException
	{
		Trigger trigger = createImmediateTrigger();
		String id = generateJobKey();
		JobDetail job = createJob(id, createRuleInstance(rule), new Date());
		log.debug("Scheduling job with id " + id + " and trigger " + trigger);
		
		ListenerManager listenerManager;
		try
		{
			listenerManager = scheduler.getListenerManager();
		} catch (SchedulerException e)
		{
			throw new InternalException("Can't schedule processing rule", e);
		}
		
		CompletableFuture<Boolean> jobExecuted = new CompletableFuture<>();
		
		listenerManager.addJobListener(new JobListenerImpl(id, jobExecuted));
		try
		{
			scheduler.scheduleJob(job, trigger);
			try
			{
				jobExecuted.get(maxWaitTimeS, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException e)
			{
				throw new InternalException("Waiting was interrupted", e);
			}			
		} catch (SchedulerException e)
		{
			throw new InternalException("Can't schedule processing rule", e);
		} finally 
		{
			listenerManager.removeJobListener(id);
		}
	}
	
	public void scheduleJob(ScheduledProcessingRule rule, Date ts)
	{
		Trigger trigger = createCronTrigger(rule);
		scheduleJob(createRuleInstance(rule), trigger, rule.getId(), ts);
	}
	
	public synchronized void undeployJob(String id)
	{
		log.debug("Removing job with id " + id);
		try
		{
			scheduler.deleteJob(new JobKey(id, BulkProcessingSupport.JOB_GROUP));
		} catch (SchedulerException e)
		{
			throw new InternalException("Can't undeploy a rule with id " + id, e);
		}
	}

	public synchronized void updateJob(ScheduledProcessingRule rule, Date ts)
	{
		Trigger trigger = createCronTrigger(rule);
		undeployJob(rule.getId());
		scheduleJob(createRuleInstance(rule), trigger, rule.getId(), ts);
	}
	
	
	private JobDetail createJob(String id, EntityTranslationRule rule, Date ts)
	{
		JobDataMap dataMap = new JobDataMap();
		dataMap.put(RULE_KEY, rule);
		dataMap.put(TS_KEY, ts);
		dataMap.put(EXECUTOR_KEY, executor);
		JobDetail job = JobBuilder.newJob(EntityRuleJob.class)
				.withIdentity(id, JOB_GROUP)
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

	private Trigger createImmediateTrigger()
	{
		return TriggerBuilder.newTrigger()
				.startNow()
				.withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.build();
	}
	
	private synchronized void scheduleJob(EntityTranslationRule rule, Trigger trigger, String id, Date ts)
	{
		JobDetail job = createJob(id, rule, ts);
		log.debug("Scheduling job with id " + id + " and trigger " + trigger);
		try
		{
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException e)
		{
			throw new InternalException("Can't schedule processing rule", e);
		}
	}
	
	private boolean filterProcessingJobs(JobDetail job)
	{
		return job.getJobDataMap().get(RULE_KEY) instanceof TranslationRule;
	}
	
	private JobDetail getJobDetail(JobKey key)
	{
		try
		{
			return scheduler.getJobDetail(key);
		} catch (SchedulerException e)
		{
			throw new InternalException("Can't retrieve job detail", e);
		}
	}	
	
	protected EntityTranslationRule createRuleInstance(TranslationRule rule)
	{
		EntityActionFactory actionFactory = actionsRegistry.getByName(rule.getAction().getName());
		EntityAction action = actionFactory.getInstance(rule.getAction().getParameters());
		
		TranslationCondition condition = new TranslationCondition(rule.getCondition());
		return new EntityTranslationRule(action, condition);
	}
	
	public static class EntityRuleJob implements Job 
	{
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException
		{
			JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
			EntityTranslationRule rule = (EntityTranslationRule) jobDataMap.get(RULE_KEY);
			BulkProcessingExecutor executor = (BulkProcessingExecutor) jobDataMap.get(EXECUTOR_KEY);
			executor.execute(rule);
		}
	}
	
	public static class RuleWithTS
	{
		public final String ruleId;
		public final Date ts;

		public RuleWithTS(String ruleId, Date ts)
		{
			this.ruleId = ruleId;
			this.ts = ts;
		}
	}
	
	private class JobListenerImpl implements JobListener
	{
		private String id;
		private CompletableFuture<Boolean> future;
		
		public JobListenerImpl(String id, CompletableFuture<Boolean> future)
		{
			this.id = id;
			this.future = future;
		}

		@Override
		public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException)
		{
			future.complete(true);
		}
		
		@Override
		public void jobToBeExecuted(JobExecutionContext context)
		{
		}
		
		@Override
		public void jobExecutionVetoed(JobExecutionContext context)
		{
		}
		
		@Override
		public String getName()
		{
			return id;
		}
	}
}
