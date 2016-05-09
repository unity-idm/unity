/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.tokens;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.Token;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

/**
 * Serializes {@link Token} to/from RDBMS {@link TokenBean} form.
 * @author K. Benedyczak
 */
@Component
public class TokenRDBMSSerializer implements RDBMSObjectSerializer<Token, TokenBean>
{
	@Override
	public TokenBean toDB(Token object)
	{
		TokenBean ret = new TokenBean(object.getValue(), object.getContents(), 
				object.getType(), object.getOwner(), object.getCreated());
		ret.setExpires(object.getExpires());
		return ret;
	}

	@Override
	public Token fromDB(TokenBean bean)
	{
		Token token = new Token(bean.getType(), bean.getName(), bean.getOwner());
		token.setContents(bean.getContents());
		token.setCreated(bean.getCreated());
		token.setExpires(bean.getExpires());
		return token;
	}
}
