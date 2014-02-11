/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;

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
	
	public AuthnRequestDocument createSAMLRequest(String idpKey);
	public SAMLSPProperties getSamlValidatorSettings();
	public AuthenticationResult verifySAMLResponse(RemoteAuthnContext authnContext) throws AuthenticationException;
}
