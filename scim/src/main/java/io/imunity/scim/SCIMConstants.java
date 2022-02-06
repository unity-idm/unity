/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim;

import java.time.Instant;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SCIMConstants
{
	public static final ObjectMapper MAPPER;

	static
	{
		MAPPER = new ObjectMapper().findAndRegisterModules();
		MAPPER.configOverride(Instant.class).setFormat(
				JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withTimeZone(TimeZone.getTimeZone("UTC")));
	}
}
