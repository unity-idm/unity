/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ComplexAttributeMapping.Builder.class)
public class ComplexAttributeMapping implements AttributeMapping
{
	public static final String id = "Complex";
	
	public final Optional<DataArray> dataArray;

	private ComplexAttributeMapping(Builder builder)
	{
		this.dataArray = builder.dataArray;
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
