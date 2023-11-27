/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers;

import java.util.Date;
import java.util.function.Function;

import io.imunity.rest.api.types.basic.RestToken;
import pl.edu.icm.unity.base.token.Token;

public class TokenMapperTest extends MapperTestBase<Token, RestToken>
{

	@Override
	protected Token getFullAPIObject()
	{
		Token token = new Token("tokenType", "tokenValue", 1L);
		token.setContents("content".getBytes());
		token.setCreated(new Date(100));
		token.setExpires(new Date(200));
		return token;
	}

	@Override
	protected RestToken getFullRestObject()
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

	@Override
	protected Pair<Function<Token, RestToken>, Function<RestToken, Token>> getMapper()
	{
		return Pair.of(TokenMapper::map, TokenMapper::map);
	}

}
