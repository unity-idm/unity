/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
class TokenVerificator
{
	private Map<String, UserAttributes> tokens = new HashMap<>();
	
	void registerUser(String token, UserAttributes userAttributes)
	{
		tokens.put(token, userAttributes);
	}
	
	boolean isPresent(String token)
	{
		return tokens.containsKey(token);
	}

	UserAttributes get(String token)
	{
		return tokens.get(token);
	}
}
