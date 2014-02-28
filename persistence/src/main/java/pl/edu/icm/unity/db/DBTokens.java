/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.mapper.TokensMapper;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.TokenBean;
import pl.edu.icm.unity.exceptions.WrongArgumentException;


/**
 * Interface allowing for manipulation of the tokens table.
 * @author K. Benedyczak
 */
@Component
public class DBTokens
{
	private DBLimits limits;
	
	@Autowired
	public DBTokens(DB db)
	{
		this.limits = db.getDBLimits();
	}

	public long addToken(String id, String type, byte[] contents, long entityId, Date expires,
			SqlSession sqlMap) throws WrongArgumentException
	{
		limits.checkNameLimit(id);
		limits.checkNameLimit(type);
		if (contents == null)
			contents = new byte[0];
		limits.checkContentsLimit(contents);
		
		TokenBean toAdd = new TokenBean(id, contents, type, entityId, new Date());
		toAdd.setExpires(expires);
		TokensMapper mapper = sqlMap.getMapper(TokensMapper.class);
		checkExists(toAdd, mapper, false);
		mapper.insertToken(toAdd);
		return toAdd.getId();
	}
	
	public void removeToken(String id, String type, SqlSession sqlMap)
			throws WrongArgumentException
	{
		TokensMapper mapper = sqlMap.getMapper(TokensMapper.class);
		TokenBean param = new TokenBean(id, type);
		checkExists(param, mapper, true);
		mapper.deleteToken(param);
	}
	
	public void updateToken(String id, String type, byte[] contents, SqlSession sqlMap) 
			throws WrongArgumentException
	{
		limits.checkNameLimit(id);
		if (contents == null)
			contents = new byte[0];
		limits.checkContentsLimit(contents);
		TokensMapper mapper = sqlMap.getMapper(TokensMapper.class);
		TokenBean updated = new TokenBean(id, type);
		updated.setContents(contents);
		checkExists(updated, mapper, true);
		mapper.updateToken(updated);
	}
	
	public TokenBean getTokenById(String type, String id, SqlSession sqlMap)
	{
		TokensMapper mapper = sqlMap.getMapper(TokensMapper.class);
		TokenBean toSelect = new TokenBean(id, type);
		return mapper.selectTokenById(toSelect);
	}
	
	public List<TokenBean> getOwnedTokens(String type, long entity, SqlSession sqlMap)
	{
		TokensMapper mapper = sqlMap.getMapper(TokensMapper.class);
		TokenBean toSelect = new TokenBean();
		toSelect.setOwner(entity);
		toSelect.setType(type);
		return mapper.selectTokensByOwner(toSelect);
	}
	
	public List<TokenBean> getExpiredTokens(SqlSession sqlMap)
	{
		TokensMapper mapper = sqlMap.getMapper(TokensMapper.class);
		return mapper.selectExpiredTokens();
	}
	
	private void checkExists(TokenBean param, TokensMapper mapper, boolean shouldExist) 
			throws WrongArgumentException
	{
		if (mapper.selectTokenById(param) == null)
		{
			if (shouldExist)
				throw new WrongArgumentException("The token " + param.getName() 
					+ " doesn't exist");
		} else
		{
			if (!shouldExist)
				throw new WrongArgumentException("The token " + param.getName() 
					+ " already exists");
		}
	}
}



