/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.tokens;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;

/**
 * Access to Tokens.xml operations
 * @author K. Benedyczak
 */
public interface TokensMapper extends BasicCRUDMapper<TokenBean>
{
	public TokenBean getById(TokenBean toSelect);
	public List<TokenBean> getByOwner(TokenBean toSelect);
	public List<TokenBean> getByType(String type);
	public List<TokenBean> getExpired();
}
