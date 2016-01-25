/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.EntityActionsRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents an installed scheduled processing rule.
 * Internally extends {@link ScheduledProcessingRuleParam} by introducing an id, which is assigned by the system
 * and uniquely identifies the rule.
 * 
 * @author K. Benedyczak
 */
public class ScheduledProcessingRule extends ScheduledProcessingRuleParam
{
	private String id;

	public ScheduledProcessingRule(String condition, EntityAction action, String cronExpression, String id)
	{
		super(condition, action, cronExpression);
		this.id = id;
	}

	public ScheduledProcessingRule(ObjectNode json, EntityActionsRegistry actionRegistry)
	{
		fromJson(json, actionRegistry);
	}

	public String getId()
	{
		return id;
	}
	
	public ObjectNode toJson(ObjectMapper mapper)
	{
		ObjectNode root = mapper.createObjectNode();
		
		root.put("id", id);
		root.put("cronExpression", getCronExpression());
		root.put("condition", getCondition());
		root.put("action", getAction().getActionDescription().getName());
		ArrayNode actionParams = root.withArray("actionParams");
		for (String param: getAction().getParameters())
			actionParams.add(param);
		
		return root;
	}
	
	private void fromJson(ObjectNode json, EntityActionsRegistry actionRegistry)
	{
		id = json.get("id").asText();
		cronExpression = json.get("cronExpression").asText();
		setCondition(json.get("condition").asText());
		String actionName = json.get("action").asText();
		ArrayNode paramsN = (ArrayNode) json.get("actionParams");
		String[] params = new String[paramsN.size()];
		for (int i=0; i<paramsN.size(); i++)
			params[i] = paramsN.get(i).asText();
		
		EntityActionFactory actionFactory;
		try
		{
			actionFactory = actionRegistry.getByName(actionName);
		} catch (IllegalTypeException e)
		{
			throw new InternalException("Can not find entity action impl for " + actionName, e);
		}
		setAction((EntityAction) actionFactory.getInstance(params));
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScheduledProcessingRule other = (ScheduledProcessingRule) obj;
		
		ObjectNode json1 = toJson(Constants.MAPPER);
		ObjectNode json2 = other.toJson(Constants.MAPPER);
		return json1.equals(json2);
	}
	
	
}
