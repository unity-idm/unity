/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authnFlow;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationDBImpl;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;

/**
 * Easy access to {@link AuthenticationFlowDefinition} storage.
 * @author P.Piernik
 */
@Component
public class AuthenticationFlowDBImpl extends GenericObjectsDAOImpl<AuthenticationFlowDefinition> implements AuthenticationFlowDB
{
	@Autowired
	public AuthenticationFlowDBImpl(AuthenticationFlowHandler handler, ObjectStoreDAO dbGeneric, AuthenticatorConfigurationDBImpl authnDAO)
	{
		super(handler, dbGeneric, AuthenticationFlowDefinition.class, "authenticatorsFlow");
		authnDAO.addRemovalHandler(this::restrictAuthenticatorRemoval);
	}
	
	private void restrictAuthenticatorRemoval(long removedId, String removedName)
	{
		List<AuthenticationFlowDefinition> flows = getAll();
		for (AuthenticationFlowDefinition flow : flows)
		{
			if (flow.getAllAuthenticators().contains(removedName))
			{

				throw new IllegalArgumentException(
						"The authenticator is used by an authentication flow "
								+ flow.getName());
			}
		}
	}
}
