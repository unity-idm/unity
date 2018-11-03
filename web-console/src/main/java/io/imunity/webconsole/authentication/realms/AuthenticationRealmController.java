/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.ControllerException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Controller for all realms view
 * 
 * @author P.Piernik
 *
 */
@Component
public class AuthenticationRealmController
{
	private RealmsManagement realmMan;
	private UnityMessageSource msg;

	@Autowired
	public AuthenticationRealmController(UnityMessageSource msg, RealmsManagement realmMan)
	{
		this.realmMan = realmMan;
		this.msg = msg;
	}

	public boolean addRealm(AuthenticationRealm realm) throws ControllerException

	{
		try
		{
			realmMan.addRealm(realm);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationRealmController.addError",
							realm.getName()),
					e.getMessage(), e);
		}

		return true;
	}

	public boolean updateRealm(AuthenticationRealm realm) throws ControllerException

	{
		try
		{
			realmMan.updateRealm(realm);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationRealmController.updateError",
							realm.getName()),
					e.getMessage(), e);
		}

		return true;
	}

	public boolean removeRealm(AuthenticationRealm realm) throws ControllerException
	{
		try
		{
			realmMan.removeRealm(realm.getName());

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationRealmController.removeError",
							realm.getName()),
					e.getMessage(), e);
		}

		return true;
	}

	public Collection<AuthenticationRealm> getRealms() throws ControllerException
	{
		try
		{
			return realmMan.getRealms();
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationRealmController.getAllError"),
					e.getMessage(), e);
		}
	}

	public AuthenticationRealm getRealm(String realmName) throws ControllerException
	{
		try
		{
			return realmMan.getRealm(realmName);
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationRealmController.getError",
							realmName),
					e.getMessage(), e);
		}
	}
}
