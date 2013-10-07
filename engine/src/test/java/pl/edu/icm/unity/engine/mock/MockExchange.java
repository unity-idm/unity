/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.authn.CredentialExchange;

public interface MockExchange extends CredentialExchange
{
	public static final String ID = "mockEx";
	public long checkPassword(String username, String password) throws IllegalIdentityValueException,
		IllegalCredentialException, IllegalTypeException, IllegalGroupValueException, EngineException;
}
