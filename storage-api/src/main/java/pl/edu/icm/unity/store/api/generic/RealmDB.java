/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;

/**
 * Easy access to {@link AuthenticationRealm} storage.
 * 
 * @author K. Benedyczak
 */
public interface RealmDB extends NamedCRUDDAOWithTS<AuthenticationRealm>
{
}
