/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implementation of {@link SecuredTokensManagement}
 * 
 * @author P.Piernik
 */
@Component
public class SecuredTokensManagementImpl implements SecuredTokensManagement
{

	private TokensManagement tokenMan;
	private AuthorizationManager authz;
	private EntityResolver idResolver;

	@Autowired
	public SecuredTokensManagementImpl(TokensManagement tokenMan, AuthorizationManager authz,
			EntityResolver idResolver)
	{
		super();
		this.tokenMan = tokenMan;
		this.authz = authz;
		this.idResolver = idResolver;
	}

	@Transactional
	@Override
	public List<Token> getAllTokens(String type)
			throws IllegalIdentityValueException, IllegalTypeException, AuthorizationException
	{
		if (checkMaintanceCapability())
		{
			return tokenMan.getAllTokens(type);
		} else
		{
			return tokenMan.getOwnedTokens(type, new EntityParam(getUserEntityId()));
		}

	}

	@Transactional
	@Override
	public List<Token> getOwnedTokens(String type, EntityParam entity)
			throws IllegalIdentityValueException, IllegalTypeException,
			AuthorizationException
	{
		if (!checkMaintanceCapability())
		{

			long entityId = idResolver.getEntityId(entity);

			if (entityId != getUserEntityId())
			{
				throw new AuthorizationException(
						"Can not get tokens owned by another user");
			}
		}
	
		return tokenMan.getOwnedTokens(type, entity);
		

	}
	
	@Transactional
	@Override
	public List<Token> getOwnedTokens(String type)
			throws IllegalIdentityValueException, IllegalTypeException,
			AuthorizationException
	{
		return tokenMan.getOwnedTokens(type, new EntityParam(getUserEntityId()));
	}

	@Transactional
	@Override
	public void removeToken(String type, String value) throws AuthorizationException
	{
		if (!checkMaintanceCapability())
		{

			Token toRemove = tokenMan.getTokenById(type, value);

			if (toRemove.getOwner() != getUserEntityId())
			{
				throw new AuthorizationException(
						"Can not remove token owned by another user");
			}
		}
		tokenMan.removeToken(type, value);

	}

	private long getUserEntityId() throws AuthorizationException
	{
		if (InvocationContext.hasCurrent())
		{
			LoginSession ls = InvocationContext.getCurrent().getLoginSession();
			if (ls != null)
				return ls.getEntityId();
		}
		throw new AuthorizationException(
				"Access is denied. The operation requires logged user");
	}

	private boolean checkMaintanceCapability()
	{
		try
		{
			authz.checkAuthorization(AuthzCapability.maintenance);
		} catch (AuthorizationException e)
		{
			return false;
		}
		return true;
	}

}
