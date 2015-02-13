/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.AuthenticatorsManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

/**
 * Implementation of {@link AuthenticatorsManagement}
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorsManagementImpl implements AuthenticatorsManagement
{
	private DBSessionManager db;
	private AuthenticatorLoader authnLoader;
	
	@Autowired
	public AuthenticatorsManagementImpl(DBSessionManager db, AuthenticatorLoader authnLoader)
	{
		super();
		this.db = db;
		this.authnLoader = authnLoader;
	}


	@Override
	public List<Map<String, BindingAuthn>> getAuthenticatorUIs(List<AuthenticationOptionDescription> authnList) 
			throws EngineException
	{
		SqlSession sql = db.getSqlSession(false);
		List<Map<String, BindingAuthn>> authenticators;
		try 
		{
			authenticators = authnLoader.getAuthenticators(authnList, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return authenticators;
	}
}
