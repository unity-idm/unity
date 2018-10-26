/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class RealmController
{
	private RealmsManagement realmMan;

	@Autowired
	public RealmController(RealmsManagement realmMan)
	{
		this.realmMan = realmMan;
	}

	public boolean addRealm(AuthenticationRealm realm) throws Exception

	{
		try
		{
			realmMan.addRealm(realm);
		} catch (Exception e)
		{
			throw e;
		}

		return true;
	}

	public boolean updateRealm(AuthenticationRealm realm) throws Exception

	{
		try
		{
			realmMan.updateRealm(realm);
		} catch (Exception e)
		{
			throw e;
		}

		return true;
	}

	public Collection<AuthenticationRealm> getRealms()
	{
		try
		{
			return realmMan.getRealms();
		} catch (EngineException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
