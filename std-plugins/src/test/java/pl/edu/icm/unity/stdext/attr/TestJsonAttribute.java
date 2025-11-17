/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.attr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class TestJsonAttribute
{
	@Test
	public void testNull() throws Exception
	{
		JsonAttributeSyntax jsonAttributeSyntax = new JsonAttributeSyntax();
		JsonNode fromNullString = jsonAttributeSyntax.convertFromString(null);
		String toStringFromNull = jsonAttributeSyntax.convertToString(fromNullString);
		assertEquals(null, toStringFromNull);
	}
}
