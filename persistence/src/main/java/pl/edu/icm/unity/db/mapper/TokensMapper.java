/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.mapper;

import java.util.List;

import pl.edu.icm.unity.db.model.TokenBean;

/**
 * Access to Tokens.xml operations
 * @author K. Benedyczak
 */
public interface TokensMapper
{
	public int insertToken(TokenBean toAdd);
	public void deleteToken(TokenBean toRemove);
	public void updateToken(TokenBean updated);
	public void updateTokenExpiration(TokenBean updated);
	public void updateTokenContents(TokenBean updated);
	public TokenBean selectTokenById(TokenBean toSelect);
	public List<TokenBean> selectTokensByOwner(TokenBean toSelect);
	public List<TokenBean> selectTokensByType(String type);
	public List<TokenBean> selectExpiredTokens();
}
