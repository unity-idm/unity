/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = SimpleAttributeMapping.Builder.class)
public class SimpleAttributeMapping implements AttributeMapping
{
	public static final String id = "Simple";

	public final Optional<DataArray> dataArray;
	public final DataValue dataValue;

	private SimpleAttributeMapping(Builder builder)
	{
		this.dataArray = builder.dataArray;
		this.dataValue = builder.dataValue;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder
	{
		private Optional<DataArray> dataArray = Optional.empty();
		private DataValue dataValue;

		private Builder()
		{
			dataValue = DataValue.builder().build();
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

		public SimpleAttributeMapping build()
		{
			return new SimpleAttributeMapping(this);
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
