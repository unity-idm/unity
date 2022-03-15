/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Optional;

public abstract class AbstractAttributeMapping
{
	public final Optional<DataArray> dataArray;

	protected AbstractAttributeMapping(BasicMappingBuilder<?> builder)
	{
		this.dataArray = builder.dataArray;
	}

	public Optional<DataArray> getDataArray()
	{
		return dataArray;
	}
	
	@SuppressWarnings("unchecked")
	public static class BasicMappingBuilder<T extends BasicMappingBuilder<?>>
	{
		
		private Optional<DataArray> dataArray = Optional.empty();

		protected BasicMappingBuilder()
		{
		}

		public T withDataArray(Optional<DataArray> dataArray)
		{
			this.dataArray = dataArray;
			return (T) this;
		}
	}

}
