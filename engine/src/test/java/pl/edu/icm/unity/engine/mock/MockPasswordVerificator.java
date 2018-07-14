/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.EntityParam;

public class MockPasswordVerificator extends AbstractLocalVerificator implements MockExchange
{
	private static final String[] ID_TYPES = {X500Identity.ID};
	private int minLen = 6;
	
	public MockPasswordVerificator(String name, String description, String exchangeId)
	{
		super(name, description, exchangeId, false);
	}

	public int getMinLen()
	{
		return minLen;
	}

	public void setMinLen(int minLen)
	{
		this.minLen = minLen;
	}


	@Override
	public String getSerializedConfiguration()
	{
		return minLen+"";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		try
		{
			minLen = Integer.parseInt(json);
		} catch (Exception e)
		{
			throw new IllegalArgumentException("The configuration of the mock password handler is invlid", e);
		}
	}

	@Override
	public String prepareCredential(String credential, String currentCredential, boolean verify)
			throws IllegalCredentialException
	{
		return "PPP" + credential;
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String credential)
	{
		if (credential == null)
			return new CredentialPublicInformation(LocalCredentialState.notSet, "");
		if (credential.startsWith("PPP"))
			return new CredentialPublicInformation(LocalCredentialState.correct, "");
		return new CredentialPublicInformation(LocalCredentialState.outdated, "");
	}

	@Override
	public long checkPassword(String username, String password)
			throws EngineException
	{
		EntityWithCredential entityWithCred = identityResolver.resolveIdentity(username, 
				ID_TYPES, getCredentialName());

		if (!password.equals(entityWithCred.getCredentialValue()))
			throw new IllegalCredentialException("Wrong password");
		return entityWithCred.getEntityId();
	}

	@Override
	public String invalidate(String currentCredential)
	{
		throw new RuntimeException("Shouldn't be called");
	}

	@Override
	public boolean isCredentialSet(EntityParam entity)
			throws EngineException
	{
		
		return true;
	}
	
	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Local;
	}

}
