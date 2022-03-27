/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.scim.console.AttributeMappingBean;
import io.imunity.scim.console.DataArrayBean;
import io.imunity.scim.console.ReferenceDataBean;

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

	@Override
	public AttributeMappingBean toBean()
	{
		AttributeMappingBean bean = new AttributeMappingBean();
		bean.setDataArray(dataArray.isEmpty() ? new DataArrayBean()
				: new DataArrayBean(dataArray.get().type, dataArray.get().value));
		bean.setDataReference(new ReferenceDataBean(type, expression));
		return bean;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(dataArray, expression, type);
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
		ReferenceAttributeMapping other = (ReferenceAttributeMapping) obj;
		return Objects.equals(dataArray, other.dataArray) && Objects.equals(expression, other.expression)
				&& type == other.type;
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
}
