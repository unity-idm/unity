/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.endpoint;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationDBImpl;
import pl.edu.icm.unity.store.objstore.authnFlow.AuthenticationFlowDBImpl;
import pl.edu.icm.unity.store.objstore.realm.RealmDBImpl;
import pl.edu.icm.unity.types.endpoint.Endpoint;

/**
 * Easy access to {@link Endpoint} storage.
 * Restricts removal of used realm and authenticators.
 * <p>
 * @author K. Benedyczak
 */
@Component
public class EndpointDBImpl extends GenericObjectsDAOImpl<Endpoint> implements EndpointDB
{
	@Autowired
	public EndpointDBImpl(EndpointHandler handler, ObjectStoreDAO dbGeneric,
			AuthenticationFlowDBImpl authnFlowDAO, AuthenticatorConfigurationDBImpl authnDAO, RealmDBImpl realmDAO)
	{
		super(handler, dbGeneric, Endpoint.class, "endpoint");
		authnDAO.addRemovalHandler(this::restrictAuthenticatorRemoval);
		authnFlowDAO.addRemovalHandler(this::restrictAuthenticationFlowRemoval);
		realmDAO.addRemovalHandler(this::restrictRealmRemoval);
	}
	
	
	private void restrictAuthenticationFlowRemoval(long removedId, String removedName)
	{
		List<Endpoint> endpoints = getAll();
		for (Endpoint endpoint: endpoints)
		{
			List<String> authnOpts = endpoint.getConfiguration()
					.getAuthenticationOptions();
			for (String ao: authnOpts)
				if (removedName.equals(ao))
					throw new IllegalArgumentException("The authentication flow is used by an endpoint " 
						+ endpoint.getName());
		}
	}	
	
	private void restrictAuthenticatorRemoval(long removedId, String removedName)
	{
		List<Endpoint> endpoints = getAll();
		for (Endpoint endpoint: endpoints)
		{
			List<String> authnOpts = endpoint.getConfiguration()
					.getAuthenticationOptions();
			for (String ao: authnOpts)
				if (removedName.equals(ao))
					throw new IllegalArgumentException("The authenticator is used by an endpoint " 
						+ endpoint.getName());
		}
	}	

	private void restrictRealmRemoval(long removedId, String removedName)
	{
		List<Endpoint> endpoints = getAll();
		for (Endpoint endpoint: endpoints)
		{
			if (endpoint.getConfiguration().getRealm().equals(removedName))
				throw new IllegalArgumentException("The realm is used by an endpoint " 
						+ endpoint.getName());
		}
	}	
}
