/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.tprofile;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBTranslationRule.Builder.class)
public class DBTranslationRule
{
	public final String condition;
	public final DBTranslationAction action;

	private DBTranslationRule(Builder builder)
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
		DBTranslationRule other = (DBTranslationRule) obj;
		return Objects.equals(action, other.action) && Objects.equals(condition, other.condition);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String condition;
		private DBTranslationAction action;

		private Builder()
		{
		}

		public Builder withCondition(String condition)
		{
			this.condition = condition;
			return this;
		}
		
		public Builder withAction(DBTranslationAction action)
		{
			this.action = action;
			return this;
		}

		public DBTranslationRule build()
		{
			return new DBTranslationRule(this);
		}
	}

}
