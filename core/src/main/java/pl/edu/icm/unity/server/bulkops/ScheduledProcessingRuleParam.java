/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops;

/**
 * Extends {@link ProcessingRule} by introducing an execution schedule.
 * 
 * @author K. Benedyczak
 */
public class ScheduledProcessingRuleParam extends ProcessingRule
{
	protected String cronExpression;

	public ScheduledProcessingRuleParam(String condition, EntityAction action, String cronExpression)
	{
		super(condition, action);
		this.cronExpression = cronExpression;
	}

	protected ScheduledProcessingRuleParam()
	{
	}
	
	public String getCronExpression()
	{
		return cronExpression;
	}

	@Override
	public String toString()
	{
		return "SchedulerProcessingRule [cronExpression=" + cronExpression
				+ ", condition=" + getCondition() + ", action="
				+ getAction() + "]";
	}
}
