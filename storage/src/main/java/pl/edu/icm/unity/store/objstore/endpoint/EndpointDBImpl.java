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
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorInstanceDBImpl;
import pl.edu.icm.unity.store.objstore.realm.RealmDBImpl;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
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
			AuthenticatorInstanceDBImpl authnDAO, RealmDBImpl realmDAO)
	{
		super(handler, dbGeneric, Endpoint.class, "endpoint");
		
		authnDAO.addRemovalHandler(this::restrictAuthenticatorRemoval);
		realmDAO.addRemovalHandler(this::restrictRealmRemoval);
	}
	
	
	private void restrictAuthenticatorRemoval(long removedId, String removedName)
	{
		List<Endpoint> endpoints = getAll();
		for (Endpoint endpoint: endpoints)
		{
			List<AuthenticationOptionDescription> authnOpts = endpoint.getConfiguration()
					.getAuthenticationOptions();
			for (AuthenticationOptionDescription ao: authnOpts)
				if (ao.getPrimaryAuthenticator().equals(removedName) || 
						ao.getMandatory2ndAuthenticator().equals(removedName))
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
