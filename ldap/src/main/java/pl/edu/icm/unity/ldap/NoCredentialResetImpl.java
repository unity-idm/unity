/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.authn.CredentialResetSettings;
import pl.edu.icm.unity.types.basic.IdentityTaV;

public class NoCredentialResetImpl implements CredentialReset
{
	private CredentialResetSettings settings;
	
	{
		settings = new CredentialResetSettings();
		settings.setEnabled(false);
	}
	@Override
	public CredentialResetSettings getSettings()
	{
		return settings;
	}

	@Override
	public void setSubject(IdentityTaV subject)
	{
	}

	@Override
	public String getSecurityQuestion()
	{
		return null;
	}

	@Override
	public void verifyStaticData(String aswer) throws WrongArgumentException,
			IllegalIdentityValueException, TooManyAttempts
	{
	}

	@Override
	public void verifyDynamicData(String emailCode) throws WrongArgumentException,
			TooManyAttempts
	{
	}

	@Override
	public void sendCode() throws EngineException
	{
	}

	@Override
	public String getCredentialConfiguration()
	{
		return null;
	}

	@Override
	public void updateCredential(String newCredential) throws EngineException
	{
	}
}