/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY;

import java.time.Instant;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

public class UnityJacksonJaxbJsonProvider extends JacksonJaxbJsonProvider
{
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static final ObjectMapper REST_MAPPER = getRestMapper();

	public UnityJacksonJaxbJsonProvider()
	{
		setMapper(REST_MAPPER);
	}

	private static ObjectMapper getRestMapper()
	{
		ObjectMapper objectMapper = new ObjectMapper()
				.configure(FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)
				.findAndRegisterModules();
		objectMapper.configOverride(Instant.class)
				.setFormat(JsonFormat.Value.forPattern(DATE_FORMAT)
						.withTimeZone(TimeZone.getTimeZone("UTC")));
		return objectMapper;
	}
}
