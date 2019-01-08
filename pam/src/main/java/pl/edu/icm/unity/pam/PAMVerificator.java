/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.credential.NoCredentialResetImpl;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;

@PrototypeComponent
public class PAMVerificator extends AbstractRemoteVerificator implements PasswordExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_PAM, PAMVerificator.class);
	
	public static final String NAME = "pam";
	public static final String IDP = "PAM";
	public static final String DESCRIPTION = "Verifies passwords using local OS PAM facility";
	
	private PAMProperties pamProperties;
	private String translationProfile;
	
	@Autowired
	public PAMVerificator(RemoteAuthnResultProcessor processor)
	{
		super(NAME, DESCRIPTION, PasswordExchange.ID, processor);
	}

	@Override
	public String getSerializedConfiguration()
	{
		StringWriter sbw = new StringWriter();
		try
		{
			pamProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize PAM verificator configuration", e);
		}
		return sbw.toString();
	}

	@Override
	public void setSerializedConfiguration(String config)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(config));
			pamProperties = new PAMProperties(properties);
			translationProfile = pamProperties.getValue(PAMProperties.TRANSLATION_PROFILE);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the PAM verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the PAM verificator(?)", e);
		}
	}

	@Override
	public AuthenticationResult checkPassword(String username, String password,
			SandboxAuthnResultCallback sandboxCallback)
	{
		RemoteAuthnState state = startAuthnResponseProcessing(sandboxCallback, 
				Log.U_SERVER_TRANSLATION, Log.U_SERVER_PAM);
		
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(
					username, password);
			return getResult(input, translationProfile, state);
		} catch (Exception e)
		{
			if (e instanceof AuthenticationException)
				log.debug("PAM authentication failed", e);
			else
				log.warn("PAM authentication failed", e);
			finishAuthnResponseProcessing(state, e);
			return new AuthenticationResult(Status.deny, null, null);
		}
	}

	private RemotelyAuthenticatedInput getRemotelyAuthenticatedInput(String username,
			String password) throws AuthenticationException, PAMException
	{
		PAM pam = new PAM("unity");
		try
		{
			UnixUser unixUser = pam.authenticate(username, password);
			return LibPAMUtils.unixUser2RAI(unixUser, IDP);
		} catch (PAMException e)
		{
			throw new AuthenticationException("PAM authentication failed", e);
		}
	}

	@Override
	public CredentialReset getCredentialResetBackend()
	{
		return new NoCredentialResetImpl();
	}
	
	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<PAMVerificator> factory)
		{
			super(NAME, DESCRIPTION, factory);
		}
	}
}
