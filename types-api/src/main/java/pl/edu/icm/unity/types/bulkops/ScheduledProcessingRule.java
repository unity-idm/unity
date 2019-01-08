/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.bulkops;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.types.NamedObject;
import pl.edu.icm.unity.types.translation.TranslationAction;

/**
 * Represents an installed scheduled processing rule.
 * Internally extends {@link ScheduledProcessingRuleParam} by introducing an id, which is assigned by the system
 * and uniquely identifies the rule.
 * 
 * @author K. Benedyczak
 */
public class ScheduledProcessingRule extends ScheduledProcessingRuleParam implements NamedObject
{
	private String id;

	public ScheduledProcessingRule(String condition, TranslationAction action, String cronExpression, String id)
	{
		super(condition, action, cronExpression);
		this.id = id;
	}

	public ScheduledProcessingRule(ScheduledProcessingRule src)
	{
		super(src);
		this.id = src.id;
	}

	@JsonCreator
	public ScheduledProcessingRule(ObjectNode json)
	{
		super(json);
		fromJson(json);
	}

	public String getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return getId();
	}
	
	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = super.toJson();
		root.put("id", id);
		return root;
	}
	
	private void fromJson(ObjectNode json)
	{
		id = json.get("id").asText();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		ScheduledProcessingRule other = (ScheduledProcessingRule) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "ScheduledProcessingRule [id=" + id + ", cronExpression=" + cronExpression + ", condition="
				+ condition + ", action=" + action + "]";
	}
}
