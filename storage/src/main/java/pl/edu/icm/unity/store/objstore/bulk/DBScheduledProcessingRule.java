/*
S * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.bulk;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.Collections;

@JsonDeserialize(builder = DBScheduledProcessingRule.Builder.class)
class DBScheduledProcessingRule
{
	public final String id;
	public final String cronExpression;
	public final String condition;
	public final String action;
	public final List<String> actionParams;

	private DBScheduledProcessingRule(Builder builder)
	{
		this.id = builder.id;
		this.cronExpression = builder.cronExpression;
		this.condition = builder.condition;
		this.action = builder.action;
		this.actionParams = Optional.ofNullable(builder.actionParams)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(action, actionParams, condition, cronExpression, id);
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
		DBScheduledProcessingRule other = (DBScheduledProcessingRule) obj;
		return Objects.equals(action, other.action) && Objects.equals(actionParams, other.actionParams)
				&& Objects.equals(condition, other.condition) && Objects.equals(cronExpression, other.cronExpression)
				&& Objects.equals(id, other.id);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String id;
		private String cronExpression;
		private String condition;
		private String action;
		private List<String> actionParams = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withId(String id)
		{
			this.id = id;
			return this;
		}

		public Builder withCronExpression(String cronExpression)
		{
			this.cronExpression = cronExpression;
			return this;
		}

		public Builder withCondition(String condition)
		{
			this.condition = condition;
			return this;
		}

		public Builder withAction(String actionName)
		{
			this.action = actionName;
			return this;
		}

		public Builder withActionParams(List<String> actionParams)
		{
			this.actionParams = actionParams;
			return this;
		}

		public DBScheduledProcessingRule build()
		{
			return new DBScheduledProcessingRule(this);
		}
	}

}
