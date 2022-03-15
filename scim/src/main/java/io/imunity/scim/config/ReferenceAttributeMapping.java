/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ReferenceAttributeMapping.Builder.class)
public class ReferenceAttributeMapping implements AttributeMapping
{
	public static final String id = "Reference";

	public enum ReferenceType
	{
		USER, GROUP, GENERIC
	}

	public final Optional<DataArray> dataArray;
	public final ReferenceType type;
	public final String expression;

	private ReferenceAttributeMapping(Builder builder)
	{
		this.dataArray = builder.dataArray;
		this.type = builder.type;
		this.expression = builder.expression;
	}

	public static Builder builder()
	{
		return new Builder();
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder
	{
		private Optional<DataArray> dataArray = Optional.empty();
		private ReferenceType type;
		private String expression;

		private Builder()
		{
		}

		public Builder withDataArray(Optional<DataArray> dataArray)
		{
			this.dataArray = dataArray;
			return this;
		}

		public Builder withType(ReferenceType type)
		{
			this.type = type;
			return this;
		}

		public Builder withExpression(String expression)
		{
			this.expression = expression;
			return this;
		}

		public ReferenceAttributeMapping build()
		{
			return new ReferenceAttributeMapping(this);
		}
	}

	@Override
	public Optional<DataArray> getDataArray()
	{
		return dataArray;
	}

	@Override
	public String getEvaluatorId()
	{
		return id;
	}

}
