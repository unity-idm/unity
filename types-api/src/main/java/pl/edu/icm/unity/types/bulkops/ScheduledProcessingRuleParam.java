/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.bulkops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * Extends {@link TranslationRule} by introducing an execution schedule.
 * 
 * @author K. Benedyczak
 */
public class ScheduledProcessingRuleParam extends TranslationRule
{
	protected String cronExpression;

	public ScheduledProcessingRuleParam(String condition, TranslationAction action, String cronExpression)
	{
		super(condition, action);
		this.cronExpression = cronExpression;
	}

	public ScheduledProcessingRuleParam(ScheduledProcessingRule src)
	{
		super(src);
		this.cronExpression = src.cronExpression;
	}

	@JsonCreator
	public ScheduledProcessingRuleParam(ObjectNode root)
	{
		fromJson(root);
	}
	
	public String getCronExpression()
	{
		return cronExpression;
	}
	
	public void setCronExpression(String cronExpression)
	{
		this.cronExpression = cronExpression;
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("cronExpression", getCronExpression());
		root.put("condition", getCondition());
		root.put("action", getAction().getName());
		ArrayNode actionParams = root.withArray("actionParams");
		for (String param: getAction().getParameters())
			actionParams.add(param);
		return root;
	}
	
	private final void fromJson(ObjectNode json)
	{
		cronExpression = json.get("cronExpression").asText();
		condition = json.get("condition").asText();
		String actionName = json.get("action").asText();
		ArrayNode paramsN = (ArrayNode) json.get("actionParams");
		String[] params = new String[paramsN.size()];
		for (int i=0; i<paramsN.size(); i++)
			params[i] = paramsN.get(i).asText();
		action = new TranslationAction(actionName, params);
	}
	
	@Override
	public String toString()
	{
		return "SchedulerProcessingRule [cronExpression=" + cronExpression
				+ ", condition=" + getCondition() + ", action="
				+ getAction() + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((cronExpression == null) ? 0 : cronExpression.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScheduledProcessingRuleParam other = (ScheduledProcessingRuleParam) obj;
		if (cronExpression == null)
		{
			if (other.cronExpression != null)
				return false;
		} else if (!cronExpression.equals(other.cronExpression))
			return false;
		return true;
	}
}
