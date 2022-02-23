/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.util.Locale;
import java.util.Set;

import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import pl.edu.icm.unity.saml.sp.web.IdPVisalSettings;

/**
 * Credential exchange between verificator and retrieval for SAML credential.
 * Credential retrieval initiates the process, verificator prepares a SAML request, retrieval 
 * (somehow) passes it to the remote IdP and gets an answer, verificator then verifies it.
 * 
 * @author K. Benedyczak
 */
public interface SAMLExchange extends CredentialExchange
{
	public static final String ID = "SAML2 exchange";
	
	RemoteAuthnContext createSAMLRequest(TrustedIdPKey idpConfigKey, String servletPath, AuthenticationStepContext authnContext,
			LoginMachineDetails initialLoginMachine, 
			String ultimateReturnURL,
			AuthenticationTriggeringContext triggeringContext);
	Set<TrustedIdPKey> getTrustedIdpKeysWithWebBindings();
	TrustedIdPs getTrustedIdPs();
	IdPVisalSettings getVisualSettings(TrustedIdPKey configKey, Locale locale);
	void destroy();
}
