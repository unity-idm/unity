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
import io.imunity.scim.console.DataValueBean;

@JsonDeserialize(builder = SimpleAttributeMapping.Builder.class)
public class SimpleAttributeMapping implements AttributeMapping
{
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder
	{
		private Optional<DataArray> dataArray = Optional.empty();
		private DataValue dataValue;

		private Builder()
		{
			dataValue = DataValue.builder().build();
		}

		public SimpleAttributeMapping build()
		{
			return new SimpleAttributeMapping(this);
		}

		public Builder withDataArray(Optional<DataArray> dataArray)
		{
			this.dataArray = dataArray;
			return this;
		}

		public Builder withDataValue(DataValue dataValue)
		{
			this.dataValue = dataValue;
			return this;
		}
	}

	public static final String id = "Simple";

	public static Builder builder()
	{
		return new Builder();
	}

	public final Optional<DataArray> dataArray;

	public final DataValue dataValue;

	private SimpleAttributeMapping(Builder builder)
	{
		this.dataArray = builder.dataArray;
		this.dataValue = builder.dataValue;
	}

	@Override
	public Optional<DataArray> getDataArray()
	{
		return dataArray;
	}

	@Override
	public AttributeMappingBean toBean()
	{
		AttributeMappingBean bean = new AttributeMappingBean();
		bean.setDataArray(dataArray.isEmpty() ? new DataArrayBean()
				: new DataArrayBean(dataArray.get().type, dataArray.get().value));
		bean.setDataValue(new DataValueBean(dataValue.type, dataValue.value));
		return bean;
	}

	@Override
	public String getEvaluatorId()
	{
		return id;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(dataArray, dataValue);
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
		SimpleAttributeMapping other = (SimpleAttributeMapping) obj;
		return Objects.equals(dataArray, other.dataArray) && Objects.equals(dataValue, other.dataValue);
	}
}
