/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;

class QueryBuilderTest
{
	@Test
	void shouldBuildEmptyQueryWhenNoParameters()
	{
		Map<String, List<String>> params = new HashMap<>();

		String result = QueryBuilder.buildQuery(params, null);

		assertEquals("", result);
	}

	@Test
	void shouldBuildQueryWithSingleParameter()
	{
		Map<String, List<String>> params = new HashMap<>();
		params.put("key1", List.of("value1"));

		String result = QueryBuilder.buildQuery(params, null);

		assertEquals("?key1=value1", result);
	}

	@Test
	void shouldBuildQueryWithMultipleValues()
	{
		Map<String, List<String>> params = new HashMap<>();
		params.put("key1", List.of("value1", "value2"));

		String result = QueryBuilder.buildQuery(params, null);

		assertAll(
			() -> assertTrue(result.contains("key1=value1")),
			() -> assertTrue(result.contains("key1=value2")),
			() -> assertTrue(result.startsWith("?"))
		);
	}

	@ParameterizedTest
	@MethodSource("contextKeyProvider")
	void shouldHandleContextKey(String contextKey, boolean shouldInclude)
	{
		Map<String, List<String>> params = new HashMap<>();

		String result = QueryBuilder.buildQuery(params, contextKey);

		assertEquals(shouldInclude,
			result.contains(LoginInProgressService.URL_PARAM_CONTEXT_KEY + "=" + contextKey));
	}

	static Stream<Arguments> contextKeyProvider()
	{
		return Stream.of(
			Arguments.of("testContext", true),
			Arguments.of(LoginInProgressService.UrlParamSignInContextKey.DEFAULT.getKey(), false),
			Arguments.of(null, false)
		);
	}

	@ParameterizedTest
	@MethodSource("specialCharProvider")
	void shouldHandleSpecialCharacters(String input, String expectedEncoded)
	{
		Map<String, List<String>> params = new HashMap<>();
		params.put("key", List.of(input));

		String result = QueryBuilder.buildQuery(params, null);

		assertTrue(result.contains("key=" + expectedEncoded));
	}

	static Stream<Arguments> specialCharProvider()
	{
		return Stream.of(
			Arguments.of("value 1", "value%201"),
			Arguments.of("value&1", "value%261"),
			Arguments.of("value?1", "value%3F1"),
			Arguments.of("value=1", "value%3D1")
		);
	}
}
