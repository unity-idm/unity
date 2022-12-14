/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;

public class RestTokenTest extends RestTypeBase<RestToken>
{

	@Override
	protected String getJson()
	{
		return "{\"type\":\"tokenType\",\"value\":\"tokenValue\",\"owner\":1,\"created\":100,"
				+ "\"expires\":200,\"contents\":\"Y29udGVudA==\"}\n";
	}

	@Override
	protected RestToken getObject()
	{
		return RestToken.builder()
				.withCreated(new Date(100))
				.withExpires(new Date(200))
				.withContents("content".getBytes())
				.withOwner(1L)
				.withValue("tokenValue")
				.withType("tokenType")
				.build();
	}

}
