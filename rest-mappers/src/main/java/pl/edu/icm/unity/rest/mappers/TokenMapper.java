/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers;

import io.imunity.rest.api.types.basic.RestToken;
import pl.edu.icm.unity.base.token.Token;

public class TokenMapper
{
	public static RestToken map(Token token)
	{
		return RestToken.builder()
				.withContents(token.getContents())
				.withCreated(token.getCreated())
				.withExpires(token.getExpires())
				.withOwner(token.getOwner())
				.withType(token.getType())
				.withValue(token.getValue())
				.build();
	}

	public static Token map(RestToken restToken)
	{
		Token token = new Token(restToken.type, restToken.value, restToken.owner);
		token.setContents(restToken.contents);
		token.setExpires(restToken.expires);
		token.setCreated(restToken.created);
		return token;
	}
}
