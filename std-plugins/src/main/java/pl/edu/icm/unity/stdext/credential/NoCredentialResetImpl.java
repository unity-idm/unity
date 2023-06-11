/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.TooManyAttempts;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;

public class NoCredentialResetImpl implements CredentialReset
{
	private PasswordCredentialResetSettings settings;
	
	{
		settings = new PasswordCredentialResetSettings();
		settings.setEnabled(false);
	}
	@Override
	public String getSettings()
	{
		ObjectNode node = Constants.MAPPER.createObjectNode();
		settings.serializeTo(node);
		return JsonUtil.toJsonString(node);
	}

	@Override
	public void setSubject(AuthenticationSubject subject)
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
	public void sendCode(String messageTemplate, boolean onlyNumberCode) throws EngineException
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

	@Override
	public Long getEntityId()
	{
		return null;
	}
}