/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.util.Locale;

import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.saml.sp.web.IdPVisalSettings;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;

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
	
	RemoteAuthnContext createSAMLRequest(String idpConfigKey, String servletPath, AuthenticationOptionKey authnOptionId);
	SAMLSPProperties getSamlValidatorSettings();
	IdPVisalSettings getVisualSettings(String configKey, Locale locale);
	void destroy();
}
