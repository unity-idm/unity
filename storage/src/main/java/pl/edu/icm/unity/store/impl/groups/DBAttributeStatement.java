/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.impl.attribute.DBAttribute;

@JsonDeserialize(builder = DBAttributeStatement.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
class DBAttributeStatement
{
	public final String condition;
	public final String extraGroupName;
	public final String resolution;
	public final DBAttribute fixedAttribute;
	public final String dynamicAttributeName;
	public final String dynamicAttributeExpression;

	private DBAttributeStatement(Builder builder)
	{
		this.condition = builder.condition;
		this.extraGroupName = builder.extraGroupName;
		this.resolution = builder.resolution;
		this.fixedAttribute = builder.fixedAttribute;
		this.dynamicAttributeName = builder.dynamicAttributeName;
		this.dynamicAttributeExpression = builder.dynamicAttributeExpression;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(condition, dynamicAttributeExpression, dynamicAttributeName, extraGroupName, fixedAttribute,
				resolution);
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
		DBAttributeStatement other = (DBAttributeStatement) obj;
		return Objects.equals(condition, other.condition)
				&& Objects.equals(dynamicAttributeExpression, other.dynamicAttributeExpression)
				&& Objects.equals(dynamicAttributeName, other.dynamicAttributeName)
				&& Objects.equals(extraGroupName, other.extraGroupName)
				&& Objects.equals(fixedAttribute, other.fixedAttribute) && Objects.equals(resolution, other.resolution);
	}

	static Builder builder()
	{
		return new Builder();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	static final class Builder
	{
		private String condition;
		private String extraGroupName;
		private String resolution;
		private DBAttribute fixedAttribute;
		private String dynamicAttributeName;
		private String dynamicAttributeExpression;

		private Builder()
		{
		}

		Builder withCondition(String condition)
		{
			this.condition = condition;
			return this;
		}

		Builder withExtraGroupName(String extraGroupName)
		{
			this.extraGroupName = extraGroupName;
			return this;
		}

		Builder withResolution(String resolution)
		{
			this.resolution = resolution;
			return this;
		}

		Builder withFixedAttribute(DBAttribute fixedAttribute)
		{
			this.fixedAttribute = fixedAttribute;
			return this;
		}

		Builder withDynamicAttributeName(String dynamicAttributeName)
		{
			this.dynamicAttributeName = dynamicAttributeName;
			return this;
		}

		Builder withDynamicAttributeExpression(String dynamicAttributeExpression)
		{
			this.dynamicAttributeExpression = dynamicAttributeExpression;
			return this;
		}

		DBAttributeStatement build()
		{
			return new DBAttributeStatement(this);
		}
	}

}
