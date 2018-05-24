/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authnFlow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;

/**
 * Easy access to {@link AuthenticationFlowDefinition} storage.
 * @author P.Piernik
 */
@Component
public class AuthenticationFlowDBImpl extends GenericObjectsDAOImpl<AuthenticationFlowDefinition> implements AuthenticationFlowDB
{
	@Autowired
	public AuthenticationFlowDBImpl(AuthenticationFlowHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, AuthenticationFlowDefinition.class, "authenticatorsFlow");
	}
}
