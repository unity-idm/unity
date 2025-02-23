/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import java.security.cert.X509Certificate;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResponseProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.credential.cert.CertificateExchange;

/**
 * Produces pseudo verificators which search for and resolve attributes of an externally verified certificate 
 * (typically via authenticated TLS).
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class LdapCertVerificator extends LdapBaseVerificator implements CertificateExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapCertVerificator.class);
	public static final String NAME = "ldap-cert";
	public static final String DESCRIPTION = "Resolves certificate subject's information using LDAPv3 protocol";
	
	@Autowired
	public LdapCertVerificator(RemoteAuthnResultTranslator processor,
			PKIManagement pkiManagement, RemoteAuthnResponseProcessor remoteAuthnProcessor)
	{
		super(NAME, DESCRIPTION, processor, pkiManagement, CertificateExchange.ID, remoteAuthnProcessor);
	}
	
	
	@Override
	public AuthenticationResult checkCertificate(X509Certificate[] chain, 
			String formForUnknown, boolean enableAssociation, AuthenticationTriggeringContext triggeringContext)
	{
		Supplier<AuthenticationResult> verificator = () -> authenticateWithCertificate(chain, formForUnknown, 
				enableAssociation, triggeringContext);
		return remoteAuthnProcessor.executeVerificator(
				verificator, 
				triggeringContext);
	}

	private AuthenticationResult authenticateWithCertificate(X509Certificate[] chain, 
			String formForUnknown, boolean enableAssociation, AuthenticationTriggeringContext triggeringContext)
	{
		try
		{
			RemotelyAuthenticatedInput input = searchRemotelyAuthenticatedInput(
					chain[0].getSubjectX500Principal().getName());
			return getResult(input, translationProfile, triggeringContext.isSandboxTriggered(), 
					formForUnknown, enableAssociation);
		} catch (Exception e)
		{
			log.debug("LDAP authentication with certificate failed", e);
			return RemoteAuthenticationResult.failed(e);
		}
	}
	
	private RemotelyAuthenticatedInput searchRemotelyAuthenticatedInput(
			String dn) throws AuthenticationException, LdapAuthenticationException
	{
		RemotelyAuthenticatedInput input = null;
		try 
		{
			input = client.search(dn, clientConfiguration);
		} catch (LdapAuthenticationException e) 
		{
			log.debug("LDAP authentication failed", e);
			throw new AuthenticationException("Authentication has failed", e);
		} catch (Exception e)
		{
			throw new AuthenticationException("Problem when authenticating against the LDAP server", e);
		}
		return input;
	}
	
	@Override
	public AuthenticationMethod getAuthenticationMethod()
	{
		return AuthenticationMethod.swk;
	}
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<LdapCertVerificator> factory)
		{
			super(NAME, DESCRIPTION, factory);
		}
	}
}
