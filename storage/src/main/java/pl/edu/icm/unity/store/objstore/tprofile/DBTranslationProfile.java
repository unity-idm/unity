/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.tprofile;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBTranslationProfile.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DBTranslationProfile
{
	public final String ver;
	public final String name;
	public final String description;
	public final String type;
	public final List<DBTranslationProfileRule> rules;
	public final String mode;

	private DBTranslationProfile(Builder builder)
	{
		this.ver = builder.ver;
		this.name = builder.name;
		this.description = builder.description;
		this.type = builder.type;
		this.rules = builder.rules;
		this.mode = builder.mode;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(description, name, mode, type, rules);
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
		DBTranslationProfile other = (DBTranslationProfile) obj;
		return Objects.equals(description, other.description) && Objects.equals(name, other.name)
				&& Objects.equals(mode, other.mode) && Objects.equals(type, other.type)
				&& Objects.equals(rules, other.rules);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String ver = "2";
		private String name;
		private String description;
		private String type = "INPUT";
		private List<DBTranslationProfileRule> rules = Collections.emptyList();
		private String mode = "DEFAULT";

		private Builder()
		{
		}

		public Builder withVer(String ver)
		{
			this.ver = ver;
			return this;
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withType(String type)
		{
			this.type = type;
			return this;
		}

		public Builder withRules(List<DBTranslationProfileRule> rules)
		{
			this.rules = rules;
			return this;
		}

		public Builder withMode(String mode)
		{
			this.mode = mode;
			return this;
		}

		public DBTranslationProfile build()
		{
			return new DBTranslationProfile(this);
		}
	}

	@JsonDeserialize(builder = DBTranslationProfileRule.Builder.class)
	public static class DBTranslationProfileRule
	{
		public final Condition condition;
		public final DBTranslationAction action;

		private DBTranslationProfileRule(Builder builder)
		{
			this.condition = builder.condition;
			this.action = builder.action;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(action, condition);
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
			DBTranslationProfileRule other = (DBTranslationProfileRule) obj;
			return Objects.equals(action, other.action) && Objects.equals(condition, other.condition);
		}

		public static Builder builder()
		{
			return new Builder();
		}

		public static final class Builder
		{
			private Condition condition;
			private DBTranslationAction action;

			private Builder()
			{
			}

			public Builder withCondition(Condition condition)
			{
				this.condition = condition;
				return this;
			}

			public Builder withAction(DBTranslationAction action)
			{
				this.action = action;
				return this;
			}

			public DBTranslationProfileRule build()
			{
				return new DBTranslationProfileRule(this);
			}
		}

	}

	@JsonDeserialize(builder = Condition.Builder.class)
	public static class Condition
	{
		public final String conditionValue;

		private Condition(Builder builder)
		{
			this.conditionValue = builder.conditionValue;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(conditionValue);
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
			Condition other = (Condition) obj;
			return Objects.equals(conditionValue, other.conditionValue);
		}

		public static Builder builder()
		{
			return new Builder();
		}

		public static final class Builder
		{
			private String conditionValue;

			@Override
			public int hashCode()
			{
				return Objects.hash(conditionValue);
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
				Builder other = (Builder) obj;
				return Objects.equals(conditionValue, other.conditionValue);
			}

			private Builder()
			{
			}

			public Builder withConditionValue(String conditionValue)
			{
				this.conditionValue = conditionValue;
				return this;
			}

			public Condition build()
			{
				return new Condition(this);
			}
		}
	}

}
