/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResponseProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.credential.NoCredentialResetImpl;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;

/**
 * Supports {@link PasswordExchange} and verifies the password and username against a configured LDAP 
 * server. Access to remote attributes and groups is also provided.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class LdapPasswordVerificator extends LdapBaseVerificator implements PasswordExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapPasswordVerificator.class);
	public static final String NAME = "ldap";
	public static final String DESCRIPTION = "Verifies password using LDAPv3 protocol";
	
	@Autowired
	public LdapPasswordVerificator(RemoteAuthnResultTranslator processor,
			PKIManagement pkiManagement, RemoteAuthnResponseProcessor remoteAuthnProcessor)
	{
		super(NAME, DESCRIPTION, processor, pkiManagement, PasswordExchange.ID, remoteAuthnProcessor);
	}
	
	@Override
	public AuthenticationResult checkPassword(String username, String password, 
			String formForUnknown, boolean enableAssociation, 
			AuthenticationTriggeringContext triggeringContext) throws AuthenticationException
	{
		Supplier<AuthenticationResult> verificator = () -> authenticateWithPassword(username, password, 
				formForUnknown, enableAssociation, triggeringContext);
		return remoteAuthnProcessor.executeVerificator(verificator, triggeringContext);
	}
	
	private AuthenticationResult authenticateWithPassword(String username, String password, 
			String formForUnknown, boolean enableAssociation, 
			AuthenticationTriggeringContext triggeringContext)
	{
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(username, password);
			RemoteAuthenticationResult result = getResult(input, translationProfile, 
					triggeringContext.isSandboxTriggered(), 
					formForUnknown, enableAssociation);
			return repackIfError(result, new ResolvableError("WebPasswordRetrieval.wrongPassword"));
		} catch (Exception e)
		{
			log.debug("LDAP authentication with password failed", e);
			return LocalAuthenticationResult.failed(e);
		}
	}
	

	private RemotelyAuthenticatedInput getRemotelyAuthenticatedInput(
			String username, String password) throws RemoteAuthenticationException
	{
		RemotelyAuthenticatedInput input = null;
		try 
		{
			input = client.bindAndSearch(username, password, clientConfiguration);
		} catch (LdapAuthenticationException e) 
		{
			log.debug("LDAP authentication failed", e);
			throw new RemoteAuthenticationException("Authentication has failed", e);
		} catch (Exception e)
		{
			throw new RemoteAuthenticationException("Problem when authenticating against the LDAP server", e);
		}
		return input;
	}	

	@Override
	public CredentialReset getCredentialResetBackend()
	{
		return new NoCredentialResetImpl();
	}
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<LdapPasswordVerificator> factory)
		{
			super(NAME, DESCRIPTION, factory);
		}
	}
}
