/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.DBTokens;
import pl.edu.icm.unity.db.model.TokenBean;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implementation of {@link TokensManagement}
 * 
 * @author K. Benedyczak
 */
@Component
public class TokensManagementImpl implements TokensManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, TokensManagementImpl.class);
	private DBSessionManager db;
	private IdentitiesResolver idResolver;
	private DBTokens dbTokens;
	private Map<String, List<TokenExpirationListener>> listeners = 
			new HashMap<String, List<TokenExpirationListener>>();
	
	@Autowired
	public TokensManagementImpl(DBSessionManager db, IdentitiesResolver idResolver,
			DBTokens dbTokens, ExecutorsService executorsService)
	{
		this.db = db;
		this.idResolver = idResolver;
		this.dbTokens = dbTokens;
		
		Runnable cleaner = new Runnable()
		{
			@Override
			public void run()
			{
				removeExpired();
			}
		};
		executorsService.getService().scheduleWithFixedDelay(cleaner, 30, 60, TimeUnit.SECONDS);
	}

	@Override
	public void addToken(String type, String value, EntityParam owner, byte[] contents,
			Date expires) throws WrongArgumentException, IllegalIdentityValueException, IllegalTypeException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entity = idResolver.getEntityId(owner, sql);
			dbTokens.addToken(value, type, contents, entity, expires, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeToken(String type, String value) throws WrongArgumentException
	{
		SqlSession sql = db.getSqlSession(false);
		try
		{
			dbTokens.removeToken(value, type, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateToken(String type, String value, byte[] contents)
			throws WrongArgumentException
	{
		SqlSession sql = db.getSqlSession(false);
		try
		{
			dbTokens.updateToken(value, type, contents, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public Token getTokenById(String type, String value) throws WrongArgumentException
	{
		SqlSession sql = db.getSqlSession(false);
		try
		{
			TokenBean token = dbTokens.getTokenById(type, value, sql);
			if (token.isExpired())
				throw new WrongArgumentException("There is no such token");
			return convert(token);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public List<Token> getOwnedTokens(String type, EntityParam owner) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		SqlSession sql = db.getSqlSession(false);
		List<TokenBean> tokens;
		try
		{
			long entity = idResolver.getEntityId(owner, sql);
			tokens = dbTokens.getOwnedTokens(type, entity, sql);
		} finally
		{
			db.releaseSqlSession(sql);
		}
		List<Token> ret = new ArrayList<>(tokens.size());
		for (TokenBean tb: tokens)
		{
			if (!tb.isExpired())
				ret.add(convert(tb));
		}
		return ret;
	}

	private Token convert(TokenBean token)
	{
		Token ret = new Token(token.getType(), token.getName(), token.getOwner());
		ret.setContents(token.getContents());
		ret.setCreated(token.getCreated());
		ret.setExpires(token.getExpires());
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
		log.debug("Removing expired tokens");
		SqlSession sql = db.getSqlSession(false);
		int removed = 0;
		try
		{
			List<TokenBean> tokens = dbTokens.getExpiredTokens(sql);
			for (TokenBean t: tokens)
			{
				List<TokenExpirationListener> l = listeners.get(t.getType());
				if (t != null)
				{
					Token tt = convert(t);
					for (TokenExpirationListener listener: l)
						listener.tokenExpired(tt);
				}
				try
				{
					dbTokens.removeToken(t.getName(), t.getType(), sql);
					removed++;
				} catch (Exception e)
				{
					log.error("Problem removing an expired token [" + t.getType() +
							"] " + t.getName(), e);
				}
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		log.debug("Removed " + removed + " tokens in this round");
	}
}
