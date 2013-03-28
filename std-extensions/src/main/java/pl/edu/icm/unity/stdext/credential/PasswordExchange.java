/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.CredentialExchange;

/**
 * Allows for exchanging a regular passphrase.
 * @author K. Benedyczak
 */
public interface PasswordExchange extends CredentialExchange
{
	public static final String ID = "password exchange";
	
	public AuthenticatedEntity checkPassword(String username, String password) throws IllegalIdentityValueException,
		IllegalCredentialException;
}
