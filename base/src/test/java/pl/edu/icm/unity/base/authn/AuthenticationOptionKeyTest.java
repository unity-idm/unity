/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.authn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.json.JsonUtil;

public class AuthenticationOptionKeyTest
{
	@Test
	public void shouldSerializeToString()
	{
		AuthenticationOptionKey key = new AuthenticationOptionKey("a", "o");
		
		String json = JsonUtil.toJsonString(key);

		assertThat(json).isEqualTo("\"a.o\"");
	}

	@Test
	public void shouldParseString()
	{
		AuthenticationOptionKey key = JsonUtil.parse("\"a.o\"", AuthenticationOptionKey.class);
		
		assertThat(key).isEqualTo(new AuthenticationOptionKey("a", "o"));
	}

	@Test
	public void shouldSerialiazeWrapped()
	{
		AuthenticationOptionKey key = new AuthenticationOptionKey("a", "o");
		Wrapper wrapper = new Wrapper(key);
		
		JsonNode jsonNode = JsonUtil.toJsonNode(wrapper);
		
		assertThat(jsonNode.get("field").asText()).isEqualTo("a.o");
	}

	
	static class Wrapper
	{
		AuthenticationOptionKey field;

		public Wrapper(AuthenticationOptionKey field)
		{
			this.field = field;
		}

		public AuthenticationOptionKey getField()
		{
			return field;
		}
	}
}
