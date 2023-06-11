/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.authn.CredentialDefinition;

/**
 * Easy access to {@link CredentialDefinition} storage.
 * 
 * @author K. Benedyczak
 */
public interface CredentialDB extends NamedCRUDDAOWithTS<CredentialDefinition>
{
}
