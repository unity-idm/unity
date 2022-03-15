/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim;

import java.time.Instant;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SCIMConstants
{
	public static final ObjectMapper MAPPER;
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	static
	{
		MAPPER = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)
				.findAndRegisterModules();
		MAPPER.configOverride(Instant.class)
				.setFormat(JsonFormat.Value.forPattern(DATE_FORMAT).withTimeZone(TimeZone.getTimeZone("UTC")));
	}
}
