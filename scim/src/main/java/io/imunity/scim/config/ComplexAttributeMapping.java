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

@JsonDeserialize(builder = ComplexAttributeMapping.Builder.class)
public class ComplexAttributeMapping implements AttributeMapping
{
	public static final String id = "Complex";

	public final Optional<DataArray> dataArray;

	private ComplexAttributeMapping(Builder builder)
	{
		this.dataArray = builder.dataArray;
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
		return bean;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(dataArray);
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
		ComplexAttributeMapping other = (ComplexAttributeMapping) obj;
		return Objects.equals(dataArray, other.dataArray);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder
	{
		private Optional<DataArray> dataArray = Optional.empty();

		private Builder()
		{
		}

		public Builder withDataArray(Optional<DataArray> dataArray)
		{
			this.dataArray = dataArray;
			return this;
		}

		public ComplexAttributeMapping build()
		{
			return new ComplexAttributeMapping(this);
		}
	}
}
