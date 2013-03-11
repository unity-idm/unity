/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.authn.AbstractLocalVerificator;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.stdext.identity.X500Identity;

public class MockPasswordVerificator extends AbstractLocalVerificator implements MockExchange
{
	private static final String[] ID_TYPES = {X500Identity.ID};
	
	public MockPasswordVerificator(String name, String description, String exchangeId)
	{
		super(name, description, exchangeId);
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
	}

	@Override
	public long checkPassword(String username, String password)
			throws IllegalIdentityValueException, IllegalCredentialException
	{
		EntityWithCredential entityWithCred = identityResolver.resolveIdentity(username, 
				ID_TYPES, getCredentialName());

		if (!password.equals(entityWithCred.getCredentialValue()))
			throw new IllegalCredentialException("Wrong password");
		return entityWithCred.getEntityId();
	}

}
