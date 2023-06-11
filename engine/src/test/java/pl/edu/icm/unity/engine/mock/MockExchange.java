/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;

public interface MockExchange extends CredentialExchange
{
	public static final String ID = "mockEx";
	public long checkPassword(String username, String password) throws IllegalIdentityValueException,
		IllegalCredentialException, IllegalTypeException, IllegalGroupValueException, EngineException;
}
