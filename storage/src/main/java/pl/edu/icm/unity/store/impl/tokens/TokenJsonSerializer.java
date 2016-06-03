/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.tokens;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;

/**
 * Serializes {@link Token} to/from DB form.
 * @author K. Benedyczak
 */
@Component
public class TokenJsonSerializer implements JsonSerializerForKryo<Token>
{
	@Autowired
	private ObjectMapper mapper;


	@Override
	public Token fromJson(ObjectNode src)
	{
		return mapper.convertValue(src, Token.class);
	}

	@Override
	public ObjectNode toJson(Token src)
	{
		return mapper.convertValue(src, ObjectNode.class);
	}

	@Override
	public Class<Token> getClazz()
	{
		return Token.class;
	}
}
