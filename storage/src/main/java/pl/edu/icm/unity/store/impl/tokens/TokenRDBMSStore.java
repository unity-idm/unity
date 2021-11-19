/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.tokens;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.store.api.TokenDAO;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;


/**
 * RDBMS storage of {@link Token}
 * @author K. Benedyczak
 */
@Repository(TokenRDBMSStore.BEAN)
public class TokenRDBMSStore extends GenericRDBMSCRUD<Token, TokenBean> implements TokenDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public TokenRDBMSStore(TokenRDBMSSerializer serializer)
	{
		super(TokensMapper.class, serializer, NAME,
				id -> new TokenNotFoundException("Token with DB key [" + id + "] does not exist"));
	}

	@Override
	public void delete(String type, String id)
	{
		TokensMapper mapper = SQLTransactionTL.getSql().getMapper(TokensMapper.class);
		TokenBean inDB = mapper.getById(new TokenBean(id, type));
		if (inDB == null)
			throw new TokenNotFoundException(elementName + " with key [" + 
					type + "//" + id + "] does not exist");
		
		mapper.deleteByKey(inDB.getId());
	}

	@Override
	public void update(Token token)
	{
		TokensMapper mapper = SQLTransactionTL.getSql().getMapper(TokensMapper.class);
		TokenBean inDB = mapper.getById(new TokenBean(token.getValue(), token.getType()));
		if (inDB == null)
			throw new TokenNotFoundException(elementName + " with key [" + 
					token.getType() + "//" + token.getValue() + "] does not exist");
		inDB.setContents(token.getContents());
		inDB.setExpires(token.getExpires());
		mapper.updateByKey(inDB);
	}

	@Override
	public Token get(String type, String id)
	{
		TokensMapper mapper = SQLTransactionTL.getSql().getMapper(TokensMapper.class);
		TokenBean inDB = mapper.getById(new TokenBean(id, type));
		if (inDB == null)
			throw new TokenNotFoundException(elementName + " with key [" + type + "//" + id +
					"] does not exist");
		return jsonSerializer.fromDB(inDB);
	}

	@Override
	public List<Token> getByType(String type)
	{
		TokensMapper mapper = SQLTransactionTL.getSql().getMapper(TokensMapper.class);
		List<TokenBean> allInDB = mapper.getByType(type);
		return convertList(allInDB);
	}

	@Override
	public List<Token> getOwned(String type, long entityId)
	{
		TokensMapper mapper = SQLTransactionTL.getSql().getMapper(TokensMapper.class);
		TokenBean selector = new TokenBean(null, type);
		selector.setOwner(entityId);
		List<TokenBean> allInDB = mapper.getByOwner(selector);
		return convertList(allInDB);
	}

	@Override
	public List<Token> getExpired()
	{
		TokensMapper mapper = SQLTransactionTL.getSql().getMapper(TokensMapper.class);
		List<TokenBean> allInDB = mapper.getExpired(new Date());
		return convertList(allInDB);
	}
}
