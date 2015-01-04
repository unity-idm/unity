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
			Date created, Date expires, Object transaction) 
			throws WrongArgumentException, IllegalIdentityValueException, IllegalTypeException
	{
		SqlSession sql = transaction == null ? db.getSqlSession(true) : (SqlSession)transaction;
		try
		{
			long entity = idResolver.getEntityId(owner, sql);
			dbTokens.addToken(value, type, contents, entity, created, expires, sql);
			if (transaction == null)
				sql.commit();
		} finally
		{
			if (transaction == null)
				db.releaseSqlSession(sql);
		}
	}
	
	@Override
	public void addToken(String type, String value, byte[] contents,
			Date created, Date expires, Object transaction) 
			throws WrongArgumentException, IllegalTypeException
	{
		SqlSession sql = transaction == null ? db.getSqlSession(true) : (SqlSession)transaction;
		try
		{
			dbTokens.addToken(value, type, contents, created, expires, sql);
			if (transaction == null)
				sql.commit();
		} finally
		{
			if (transaction == null)
				db.releaseSqlSession(sql);
		}
	}
	
	@Override
	public void removeToken(String type, String value, Object transaction) throws WrongArgumentException
	{
		SqlSession sql = transaction == null ? db.getSqlSession(true) : (SqlSession)transaction;
		try
		{
			dbTokens.removeToken(value, type, sql);
			if (transaction == null)
				sql.commit();
		} finally
		{
			if (transaction == null)
				db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateToken(String type, String value, Date expires, byte[] contents, Object transaction)
			throws WrongArgumentException
	{
		SqlSession sql = transaction == null ? db.getSqlSession(true) : (SqlSession)transaction;
		try
		{
			dbTokens.updateToken(value, type, expires, contents, sql);
			if (transaction == null)
				sql.commit();
		} finally
		{
			if (transaction == null)
				db.releaseSqlSession(sql);
		}
	}

	@Override
	public Token getTokenById(String type, String value, Object transaction) throws WrongArgumentException
	{
		SqlSession sql = transaction == null ? db.getSqlSession(true) : (SqlSession)transaction;
		try
		{
			TokenBean token = dbTokens.getTokenById(type, value, sql);
			if (transaction == null)
				sql.commit();
			if (token.isExpired())
				throw new WrongArgumentException("There is no such token");
			return convert(token);
		} finally
		{
			if (transaction == null)
				db.releaseSqlSession(sql);
		}
	}

	@Override
	public List<Token> getOwnedTokens(String type, EntityParam owner, Object transaction) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		SqlSession sql = transaction == null ? db.getSqlSession(true) : (SqlSession)transaction;
		List<TokenBean> tokens;
		try
		{
			long entity = idResolver.getEntityId(owner, sql);
			tokens = dbTokens.getOwnedTokens(type, entity, sql);
			if (transaction == null)
				sql.commit();
		} finally
		{
			if (transaction == null)
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
		SqlSession sql = db.getSqlSession(true);
		int removed = 0;
		try
		{
			List<TokenBean> tokens = dbTokens.getExpiredTokens(sql);
			for (TokenBean t: tokens)
			{
				List<TokenExpirationListener> l = listeners.get(t.getType());
				if (l != null)
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

	@Override
	public Object startTokenTransaction()
	{
		return db.getSqlSession(true);
	}

	@Override
	public void commitTokenTransaction(Object transaction)
	{
		((SqlSession)transaction).commit();
	}

	@Override
	public void closeTokenTransaction(Object transaction)
	{
		db.releaseSqlSession((SqlSession) transaction);
	}

	@Override
	public void addToken(String type, String value, EntityParam owner, byte[] contents,
			Date created, Date expires) throws WrongArgumentException, IllegalIdentityValueException,
			IllegalTypeException
	{
		addToken(type, value, owner, contents, created, expires, null);
	}
	
	@Override
	public void addToken(String type, String value, byte[] contents,
			Date created, Date expires) throws WrongArgumentException, IllegalTypeException
	{
		addToken(type, value, contents, created, expires, null);
	}

	@Override
	public void removeToken(String type, String value) throws WrongArgumentException
	{
		removeToken(type, value, null);
	}

	@Override
	public void updateToken(String type, String value, Date expires, byte[] contents)
			throws WrongArgumentException
	{
		updateToken(type, value, expires, contents, null);
	}

	@Override
	public Token getTokenById(String type, String value) throws WrongArgumentException
	{
		return getTokenById(type, value, null);
	}

	@Override
	public List<Token> getOwnedTokens(String type, EntityParam entity)
			throws IllegalIdentityValueException, IllegalTypeException
	{
		return getOwnedTokens(type, entity, null);
	}

	@Override
	public List<Token> getAllTokens(String type)
	{
		SqlSession sql = db.getSqlSession(true);
		List<Token> ret;
		try
		{
			List<TokenBean> tokens = dbTokens.getTokens(type, sql);
			ret = new ArrayList<>(tokens.size());
			for (TokenBean t: tokens)
			{
				Token tt = convert(t);
				ret.add(tt);
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return ret;
	}
}
