/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import java.util.function.Supplier;

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
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResponseProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.credential.NoCredentialResetImpl;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;

@PrototypeComponent
public class PAMVerificator extends AbstractRemoteVerificator implements PasswordExchange
{
	private static final ResolvableError GENERIC_ERROR = new ResolvableError("WebPasswordRetrieval.wrongPassword");

	private static final Logger log = Log.getLogger(Log.U_SERVER_PAM, PAMVerificator.class);
	
	public static final String NAME = "pam";
	public static final String IDP = "PAM";
	public static final String DESCRIPTION = "Verifies passwords using local OS PAM facility";
	
	private final RemoteAuthnResponseProcessor remoteAuthnProcessor;
	private PAMProperties pamProperties;
	private TranslationProfile translationProfile;

	
	@Autowired
	public PAMVerificator(RemoteAuthnResultTranslator translator, RemoteAuthnResponseProcessor remoteAuthnProcessor)
	{
		super(NAME, DESCRIPTION, PasswordExchange.ID, translator);
		this.remoteAuthnProcessor = remoteAuthnProcessor;
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
			translationProfile = getTranslationProfile(
					pamProperties, CommonWebAuthnProperties.TRANSLATION_PROFILE,
					CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE);
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
			String formForUnknown, boolean enableAssociation, 
			AuthenticationTriggeringContext triggeringContext)
	{
		Supplier<AuthenticationResult> verificator = () -> authenticate(username, password, 
				formForUnknown, enableAssociation, triggeringContext);
		return remoteAuthnProcessor.executeVerificator(verificator, triggeringContext);
	}

	private AuthenticationResult authenticate(String username, String password,
			String formForUnknown, boolean enableAssociation, 
			AuthenticationTriggeringContext triggeringContext)
	{
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(
					username, password);
			RemoteAuthenticationResult result = getResult(input, translationProfile, 
					triggeringContext.isSandboxTriggered(), 
					formForUnknown, enableAssociation);
			return repackIfError(result, GENERIC_ERROR);
		} catch (Exception e)
		{
			if (e instanceof AuthenticationException)
				log.info("PAM authentication failed", e);
			else
				log.warn("PAM authentication failed", e);
			return LocalAuthenticationResult.failed(GENERIC_ERROR, e);
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
