/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.cert;

import java.security.cert.X509Certificate;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;

/**
 * Exchange for checking if the presented certificate is in the DB. It is assumed that the
 * certificate was actually authenticated by the transport layer - the verificator only checks 
 * if the certificate is present in the database.
 * @author K. Benedyczak
 */
public interface CertificateExchange extends CredentialExchange
{
	public static final String ID = "certificate exchange";
	
	AuthenticationResult checkCertificate(X509Certificate[] chain, SandboxAuthnResultCallback sandboxCallback); 
}
