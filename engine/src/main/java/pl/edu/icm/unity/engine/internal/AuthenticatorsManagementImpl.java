/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.authn.AuthenticatorInstanceDB;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.AuthenticatorsManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

/**
 * Implementation of {@link AuthenticatorsManagement}
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorsManagementImpl implements AuthenticatorsManagement
{
	private AuthenticatorLoader authnLoader;
	private AuthenticatorInstanceDB authenticatorDB;
	
	@Autowired
	public AuthenticatorsManagementImpl(AuthenticatorLoader authnLoader,
			AuthenticatorInstanceDB authenticatorDB)
	{
		this.authnLoader = authnLoader;
		this.authenticatorDB = authenticatorDB;
	}


	@Override
	@Transactional(noTransaction=true)
	public List<AuthenticationOption> getAuthenticatorUIs(List<AuthenticationOptionDescription> authnList) 
			throws EngineException
	{
		return authnLoader.getAuthenticators(authnList, SqlSessionTL.get());
	}
	
	@Override
	@Transactional(noTransaction=true)
	public void removeAllPersistedAuthenticators() throws EngineException
	{
		authenticatorDB.removeAllNoCheck(SqlSessionTL.get());
	}
}
