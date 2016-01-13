/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.Properties;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.springframework.stereotype.Component;

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
/*
	{
		// TODO Auto-generated method stub
		CronExpression expression = new CronExpression(cronExpression)
		Trigger trigger = TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build();
		JobDetail job = JobBuilder.newJob().ofType(jobClazz).build();
		sched.scheduleJob(job, trigger);
	}
*/
	@Override
	public void applyRule(ProcessingRule rule)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scheduleRule(ScheduledProcessingRuleParam rule)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeScheduledRule(String id)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateScheduledRule(ScheduledProcessingRule rule)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<ScheduledProcessingRule> getScheduledRules()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
