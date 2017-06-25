/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.types.authn.AuthenticatorInstance;

/**
 * Easy access to {@link AuthenticatorInstance} storage.
 * @author K. Benedyczak
 */
public interface AuthenticatorInstanceDB extends NamedCRUDDAOWithTS<AuthenticatorInstance>
{
}
