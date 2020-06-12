/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.EntityParam;

@PrototypeComponent
class OTPVerificator extends AbstractLocalVerificator implements OTPExchange 
{
	public static final String DESC = "One-time password";
	public static final String[] IDENTITY_TYPES = {UsernameIdentity.ID, EmailIdentity.ID};

	
	public OTPVerificator()
	{
		super(OTP.NAME, DESC, OTPExchange.ID, true);
	}
	
	@Override
	public String getExchangeId()
	{
		return OTPExchange.ID;
	}

	@Override
	public AuthenticationResult verifyCode(String codeFromUser, String username,
			SandboxAuthnResultCallback sandboxCallback)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OTPCredentialReset getCredentialResetBackend()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String prepareCredential(String rawCredential, String currentCredential, boolean verifyNew)
			throws IllegalCredentialException, InternalException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential) throws InternalException
	{
		return new CredentialPublicInformation(LocalCredentialState.notSet, "");
		// TODO Auto-generated method stub
	}

	@Override
	public String invalidate(String currentCredential)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCredentialSet(EntityParam entity) throws EngineException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCredentialDefinitionChagneOutdatingCredentials(String newCredentialDefinition)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSerializedConfiguration()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSerializedConfiguration(String config)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getCodeLength()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Component
	public static class Factory extends AbstractLocalCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<OTPVerificator> factory)
		{
			super(OTP.NAME, DESC, false, factory);
		}
	}
}
