/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.store.api.TokenDAO;
import pl.edu.icm.unity.store.api.TokenDAO.TokenNotFoundException;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@Component
public class TokensManagementImpl implements TokensManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, TokensManagementImpl.class);
	private EntityResolver idResolver;
	private TokenDAO dbTokens;
	private TransactionalRunner tx; 
	private Map<String, List<TokenExpirationListener>> listeners = 
			new HashMap<String, List<TokenExpirationListener>>();
	
	@Autowired
	public TokensManagementImpl(EntityResolver idResolver, TransactionalRunner tx,
			TokenDAO dbTokens, ExecutorsService executorsService)
	{
		this.idResolver = idResolver;
		this.tx = tx;
		this.dbTokens = dbTokens;
		
		Runnable cleaner = new Runnable()
		{
			@Override
			public void run()
			{
				removeExpired();
			}
		};
		executorsService.getScheduledService().scheduleWithFixedDelay(cleaner, 30, 60, TimeUnit.SECONDS);
	}

	@Transactional
	@Override
	public void addToken(String type, String value, EntityParam owner, byte[] contents,
			Date created, Date expires) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		long entity = idResolver.getEntityId(owner);
		addTokenInternal(type, value, contents, created, expires, entity);
	}
	
	@Transactional
	@Override
	public void addToken(String type, String value, byte[] contents,
			Date created, Date expires) 
			throws IllegalTypeException
	{
		addTokenInternal(type, value, contents, created, expires, null);
	}
	
	private void addTokenInternal(String type, String value, byte[] contents,
			Date created, Date expires, Long entity)
	{
		Token token = new Token(type, value, entity);
		token.setContents(contents);
		token.setCreated(created);
		token.setExpires(expires);
		dbTokens.create(token);
	}
	
	@Transactional
	@Override
	public void removeToken(String type, String value)
	{
		dbTokens.delete(type, value);
	}

	@Transactional
	@Override
	public void updateToken(String type, String value, Date expires, byte[] contents)
	{
		Token token = getTokenById(type, value);
		if (contents != null)
			token.setContents(contents);
		if (expires != null)
			token.setExpires(expires);		
		dbTokens.update(token);
	}

	@Transactional(autoCommit=false)
	@Override
	public Token getTokenById(String type, String value)
	{
		Token token = dbTokens.get(type, value);
		if (token.isExpired())
			throw new TokenNotFoundException();
		return token;
	}
	
	@Transactional
	@Override
	public List<Token> getOwnedTokens(String type, EntityParam owner) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		long entity = idResolver.getEntityId(owner);
		List<Token> tokens = dbTokens.getOwned(type, entity);
		return filterExpired(tokens);
	}
	
	@Transactional
	@Override
	public List<Token> getAllTokens(String type)
	{
		List<Token> tokens = dbTokens.getByType(type);
		return filterExpired(tokens);
	}
	
	@Transactional
	@Override
	public List<Token> getAllTokens()
	{
		List<Token> tokens = dbTokens.getAll();
		return filterExpired(tokens);
	}


	private List<Token> filterExpired(List<Token> tokens)
	{
		List<Token> ret = new ArrayList<>(tokens.size());;
		for (Token t: tokens)
			if (!t.isExpired())
				ret.add(t);
		return ret;
	}
	
	@Override
	public synchronized void addTokenExpirationListener(TokenExpirationListener listener, String type)
	{
		List<TokenExpirationListener> l = listeners.get(type);
		if (l == null)
		{
			l = new ArrayList<>();
			listeners.put(type, l);
		}
		l.add(listener);
	}
	
	private synchronized void removeExpired()
	{
		tx.runInTransaction(() -> {
			transactionalRemoveExpired();
		});
	}

	private void transactionalRemoveExpired()
	{
		log.debug("Removing expired tokens");
		int removed = 0;
		
		List<Token> tokens = dbTokens.getExpired();
		for (Token t: tokens)
		{
			List<TokenExpirationListener> l = listeners.get(t.getType());
			if (l != null)
			{
				for (TokenExpirationListener listener: l)
					listener.tokenExpired(t);
			}
			try
			{
				dbTokens.delete(t.getType(), t.getValue());
				removed++;
			} catch (Exception e)
			{
				log.error("Problem removing an expired token [" + t.getType() +
						"] " + t.getValue(), e);
			}
		}
		log.debug("Removed " + removed + " tokens in this round");
	}
}
