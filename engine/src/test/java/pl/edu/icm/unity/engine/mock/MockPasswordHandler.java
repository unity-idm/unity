/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.authn.AbstractLocalCredentialHandler;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Mock password verificator. Password is stored in db in plain text. config is used to provide minimum length.
 * @author K. Benedyczak
 */
public class MockPasswordHandler extends AbstractLocalCredentialHandler
{
	private int minLen = 6;
	
	public MockPasswordHandler(CredentialType credType)
	{
		super(credType);
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
	public String prepareCredential(String credential, String currentCredential)
			throws IllegalCredentialException
	{
		return "PPP" + credential;
	}

	@Override
	public LocalCredentialState checkCredentialState(String credential)
	{
		if (credential == null)
			return LocalCredentialState.notSet;
		if (credential.startsWith("PPP"))
			return LocalCredentialState.correct;
		return LocalCredentialState.outdated;
	}

}
