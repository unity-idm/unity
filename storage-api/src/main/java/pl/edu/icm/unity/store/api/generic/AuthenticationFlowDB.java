/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;

/**
 * Easy access to {@link AuthenticationFlowDefinition} storage.
 * 
 * @author P.Piernik
 */
public interface AuthenticationFlowDB extends NamedCRUDDAOWithTS<AuthenticationFlowDefinition>
{
}
