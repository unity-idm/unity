/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.common.EndpointController;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all realms views
 * 
 * @author P.Piernik
 *
 */
@Component
class AuthenticationRealmsController
{
	private RealmsManagement realmMan;
	private UnityMessageSource msg;
	private EndpointController endpointController;

	@Autowired
	AuthenticationRealmsController(UnityMessageSource msg, RealmsManagement realmMan,
			EndpointController endpointController)
	{
		this.realmMan = realmMan;
		this.msg = msg;
		this.endpointController = endpointController;
	}

	void addRealm(AuthenticationRealm realm) throws ControllerException
	{
		try
		{
			realmMan.addRealm(realm);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationRealmController.addError", realm.getName()),
					e.getMessage(), e);
		}

	}

	void updateRealm(AuthenticationRealm realm) throws ControllerException

	{
		try
		{
			realmMan.updateRealm(realm);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationRealmController.updateError", realm.getName()),
					e.getMessage(), e);
		}
	}

	void removeRealm(AuthenticationRealm realm) throws ControllerException
	{
		try
		{
			realmMan.removeRealm(realm.getName());

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationRealmController.removeError", realm.getName()),
					e.getMessage(), e);
		}
	}

	Collection<AuthenticationRealmEntry> getRealms() throws ControllerException
	{

		List<AuthenticationRealmEntry> ret = new ArrayList<>();
		Collection<AuthenticationRealm> realms;
		try
		{
			realms = realmMan.getRealms();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AuthenticationRealmController.getAllError"),
					e.getMessage(), e);
		}
		List<ResolvedEndpoint> endpoints = endpointController.getEndpoints();

		for (AuthenticationRealm realm : realms)
		{
			ret.add(new AuthenticationRealmEntry(realm, filterEndpoints(realm.getName(), endpoints)));
		}

		return ret;

	}

	AuthenticationRealmEntry getRealm(String realmName) throws ControllerException
	{
		List<ResolvedEndpoint> endpoints =  endpointController.getEndpoints();

		try
		{
			return new AuthenticationRealmEntry(realmMan.getRealm(realmName), filterEndpoints(realmName, endpoints));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationRealmController.getError", realmName),
					e.getMessage(), e);
		}
	}

	private List<String> filterEndpoints(String realmName, List<ResolvedEndpoint> all)
	{
		return all.stream().filter(e -> e.getRealm().getName().equals(realmName)).map(e -> e.getName()).sorted()
				.collect(Collectors.toList());
	}
}
