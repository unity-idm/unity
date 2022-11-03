/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.ParameterizedType;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract  class RestTypeBase<T>
{
	protected final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
	
	private final Class<T> type;
	
	@SuppressWarnings("unchecked")
	public RestTypeBase()
	{
		this.type = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
	}

	@Test
	public void shouldDeserializeFullObject() throws JsonMappingException, JsonProcessingException
	{
		assertThat(MAPPER.readValue(getJson(), type), is(getObject()));
	}

	@Test
	public void serializationIsIdempotent() throws JsonMappingException, JsonProcessingException
	{
		assertThat(getObject(),
				is(MAPPER.readValue(MAPPER.writeValueAsString(getObject()), type)));
	}
	
	
	protected abstract String getJson();

	protected abstract T getObject();

	
}
