/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;

/**
 * Easy access to {@link AuthenticatorConfiguration} storage.
 * @author K. Benedyczak
 */
public interface AuthenticatorInstanceDB extends NamedCRUDDAOWithTS<AuthenticatorConfiguration>
{
}
