/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops;

/**
 * Represents an installed scheduled processing rule.
 * Internally extends {@link ScheduledProcessingRuleParam} by introducing an id, which is assigned by the system
 * and uniquely identifies the rule.
 * 
 * @author K. Benedyczak
 */
public class ScheduledProcessingRule extends ScheduledProcessingRuleParam
{
	private String cronExpression;
	private String id;

	public ScheduledProcessingRule(String condition, EntityAction action, String cronExpression, String id)
	{
		super(condition, action, cronExpression);
		this.id = id;
	}

	public String getCronExpression()
	{
		return cronExpression;
	}

	public String getId()
	{
		return id;
	}
}
