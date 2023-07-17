/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.ParameterizedType;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class DBTypeTestBase<T>
{
	protected final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

	private final Class<T> type;

	@SuppressWarnings("unchecked")
	public DBTypeTestBase()
	{
		this.type = (Class<T>) ((ParameterizedType) this.getClass()
				.getGenericSuperclass()).getActualTypeArguments()[0];
	}

	@Test
	public void shouldDeserializeFullObject() throws JsonMappingException, JsonProcessingException
	{
		T fromJson = MAPPER.readValue(getJson(), type);
		T object = getObject();
		assertThat(fromJson).isEqualTo(object);
	}

//	@Test
//	public void serializationIsIdempotent() throws JsonMappingException, JsonProcessingException
//	{
//		T object = getObject();
//		String json = MAPPER.writeValueAsString(object);
//		T fromJson = MAPPER.readValue(json, type);
//		assertThat(object).isEqualTo(fromJson);
//	}

	protected abstract String getJson();

	protected abstract T getObject();

}
