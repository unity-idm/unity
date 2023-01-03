/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestAttributeStatement.Builder.class)

public class RestAttributeStatement
{
	public final String condition;
	public final String extraGroupName;
	public final String resolution;
	public final RestAttribute fixedAttribute;
	public final String dynamicAttributeName;
	public final String dynamicAttributeExpression;

	private RestAttributeStatement(Builder builder)
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
		return Objects.hash(condition, dynamicAttributeExpression, dynamicAttributeName, extraGroupName,
				fixedAttribute, resolution);
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
		RestAttributeStatement other = (RestAttributeStatement) obj;
		return Objects.equals(condition, other.condition)
				&& Objects.equals(dynamicAttributeExpression, other.dynamicAttributeExpression)
				&& Objects.equals(dynamicAttributeName, other.dynamicAttributeName)
				&& Objects.equals(extraGroupName, other.extraGroupName)
				&& Objects.equals(fixedAttribute, other.fixedAttribute) && Objects.equals(resolution, other.resolution);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String condition;
		private String extraGroupName;
		private String resolution;
		private RestAttribute fixedAttribute;
		private String dynamicAttributeName;
		private String dynamicAttributeExpression;

		private Builder()
		{
		}

		public Builder withCondition(String condition)
		{
			this.condition = condition;
			return this;
		}

		public Builder withExtraGroupName(String extraGroupName)
		{
			this.extraGroupName = extraGroupName;
			return this;
		}

		public Builder withResolution(String resolution)
		{
			this.resolution = resolution;
			return this;
		}

		public Builder withFixedAttribute(RestAttribute fixedAttribute)
		{
			this.fixedAttribute = fixedAttribute;
			return this;
		}

		public Builder withDynamicAttributeName(String dynamicAttributeName)
		{
			this.dynamicAttributeName = dynamicAttributeName;
			return this;
		}

		public Builder withDynamicAttributeExpression(String dynamicAttributeExpression)
		{
			this.dynamicAttributeExpression = dynamicAttributeExpression;
			return this;
		}

		public RestAttributeStatement build()
		{
			return new RestAttributeStatement(this);
		}
	}

	

}
