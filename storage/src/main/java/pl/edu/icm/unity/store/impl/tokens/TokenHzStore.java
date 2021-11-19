/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.tokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.TransactionalMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.PredicateBuilder;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.store.api.TokenDAO;
import pl.edu.icm.unity.store.hz.GenericBasicHzCRUD;


/**
 * Hazelcast implementation of token store.
 * 
 * @author K. Benedyczak
 */
@Repository(TokenHzStore.STORE_ID)
public class TokenHzStore extends GenericBasicHzCRUD<Token> implements TokenDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Autowired
	public TokenHzStore(TokenRDBMSStore rdbmsDAO)
	{
		super(STORE_ID, NAME, TokenRDBMSStore.BEAN, rdbmsDAO);
	}

	@Override
	public void delete(String type, String id)
	{
		PredicateBuilder pBuilder = getPredicate(type, null, id);
		TransactionalMap<Long, Token> hMap = getMap();
		Collection<Long> keys = hMap.keySet(pBuilder);
		if (keys.isEmpty())
			throw new TokenNotFoundException("Token with key [" + type + "//" + id +
					"] does not exist");
		deleteByKey(keys.iterator().next());
	}

	@Override
	public void update(Token token)
	{
		PredicateBuilder pBuilder = getPredicate(token.getType(), null, token.getValue());
		TransactionalMap<Long, Token> hMap = getMap();
		Collection<Long> keys = hMap.keySet(pBuilder);
		if (keys.isEmpty())
			throw new TokenNotFoundException("Token with key [" + 
					token.getType() + "//" + token.getValue() + "] does not exist");
		updateByKey(keys.iterator().next(), token);
	}

	@Override
	public Token get(String type, String id)
	{
		PredicateBuilder pBuilder = getPredicate(type, null, id);
		TransactionalMap<Long, Token> hMap = getMap();
		Collection<Token> values = hMap.values(pBuilder);
		if (values.isEmpty())
			throw new TokenNotFoundException("Token with key [" + type + "//" + id +
					"] does not exist");
		return values.iterator().next();
	}

	@Override
	public List<Token> getByType(String type)
	{
		PredicateBuilder pBuilder = getPredicate(type, null, null);
		return getByPredicate(pBuilder);
	}

	@Override
	public List<Token> getOwned(String type, long entityId)
	{
		PredicateBuilder pBuilder = getPredicate(type, entityId, null);
		return getByPredicate(pBuilder);
	}

	@Override
	public List<Token> getExpired()
	{
		EntryObject e = new PredicateBuilder().getEntryObject();
		PredicateBuilder pBuilder = e.get("expires").lessThan(new Date());
		return getByPredicate(pBuilder);
	}

	private List<Token> getByPredicate(PredicateBuilder pBuilder)
	{
		TransactionalMap<Long, Token> hMap = getMap();
		Collection<Token> values = hMap.values(pBuilder);
		return new ArrayList<>(values);
	}
	
	private PredicateBuilder getPredicate(String type, Long owner, String value)
	{
		EntryObject e = new PredicateBuilder().getEntryObject();
		PredicateBuilder pBuilder = null;
		if (owner != null)
			pBuilder = safeAdd(pBuilder, e.get("owner").equal(owner));
		if (type != null)
			pBuilder = safeAdd(pBuilder, e.get("type").equal(type));
		if (value != null)
			pBuilder = safeAdd(pBuilder, e.get("value").equal(value));
		return pBuilder;
	}
	
	private PredicateBuilder safeAdd(PredicateBuilder existing, PredicateBuilder condition)
	{
		return existing == null ? condition : existing.and(condition);
	}
}
