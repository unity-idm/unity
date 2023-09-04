/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.realms;

import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
class RealmsController
{
	private final RealmsManagement realmMan;
	private final MessageSource msg;
	private final EndpointManagement endpointMan;
	private final NotificationPresenter notificationPresenter;

	RealmsController(MessageSource msg, RealmsManagement realmMan,
					 EndpointManagement endpointMan, NotificationPresenter notificationPresenter)
	{
		this.realmMan = realmMan;
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.notificationPresenter = notificationPresenter;
	}

	void addRealm(AuthenticationRealm realm)
	{
		try
		{
			realmMan.addRealm(realm);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationRealmController.addError", realm.getName()), e.getMessage());
		}

	}

	void updateRealm(AuthenticationRealm realm)

	{
		try
		{
			realmMan.updateRealm(realm);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationRealmController.updateError", realm.getName()), e.getMessage());
		}
	}

	void removeRealm(AuthenticationRealm realm)
	{
		try
		{
			realmMan.removeRealm(realm.getName());

		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationRealmController.removeError", realm.getName()), e.getMessage());
		}
	}

	Collection<AuthenticationRealmEntry> getRealms()
	{

		List<AuthenticationRealmEntry> ret = new ArrayList<>();
		Collection<AuthenticationRealm> realms;
		try
		{
			realms = realmMan.getRealms();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationRealmController.getAllError"), e.getMessage());
			return List.of();
		}
		List<ResolvedEndpoint> endpoints = getEndpoints();

		for (AuthenticationRealm realm : realms)
		{
			ret.add(new AuthenticationRealmEntry(realm, filterEndpoints(realm.getName(), endpoints)));
		}

		return ret;

	}

	AuthenticationRealmEntry getRealm(String realmName)
	{
		List<ResolvedEndpoint> endpoints =  getEndpoints();

		try
		{
			return new AuthenticationRealmEntry(realmMan.getRealm(realmName), filterEndpoints(realmName, endpoints));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationRealmController.getError", realmName), e.getMessage());
		}
		return null;
	}

	private List<String> filterEndpoints(String realmName, List<ResolvedEndpoint> all)
	{
		return all.stream().filter(e -> e.getRealm() != null && e.getRealm().getName().equals(realmName)).map(e -> e.getName()).sorted()
				.collect(Collectors.toList());
	}

	List<ResolvedEndpoint> getEndpoints()
	{
		try
		{
			return endpointMan.getDeployedEndpoints();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("EndpointController.getAllError"), e.getMessage());
		}
		return List.of();
	}
}
