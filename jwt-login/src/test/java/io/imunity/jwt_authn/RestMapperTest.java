/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt_authn;

import static io.imunity.jwt.UnityJacksonJaxbJsonProvider.REST_MAPPER;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class RestMapperTest
{
	@Test
	void shouldSerialize() throws JsonProcessingException
	{
		String strValue = REST_MAPPER.writeValueAsString(new TestDTO("value"));
		
		Assertions.assertEquals(strValue, "{\"field\":\"value\"}");
	}
	
	@Test
	void shouldDeserialize() throws JsonProcessingException
	{
		TestDTO testValue = REST_MAPPER.readValue("{\"field\":\"value\"}", TestDTO.class);
		
		Assertions.assertEquals(testValue, new TestDTO("value"));
	}
}

