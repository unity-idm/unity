/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.ParameterizedType;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class RestTypeBase<T>
{
	protected final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

	private final Class<T> type;

	@SuppressWarnings("unchecked")
	public RestTypeBase()
	{
		this.type = (Class<T>) ((ParameterizedType) this.getClass()
				.getGenericSuperclass()).getActualTypeArguments()[0];
	}

	@Test
	public void shouldDeserializeFullObject() throws JsonMappingException, JsonProcessingException
	{
		T fromJson = MAPPER.readValue(getJson(), type);
		assertThat(fromJson).isEqualTo(getObject());
	}

	@Test
	public void serializationIsIdempotent() throws JsonMappingException, JsonProcessingException
	{
		String json = MAPPER.writeValueAsString(getObject());
		T fromJson = MAPPER.readValue(json, type);
		assertThat(getObject()).isEqualTo(fromJson);
	}

	protected abstract String getJson();

	protected abstract T getObject();

}
