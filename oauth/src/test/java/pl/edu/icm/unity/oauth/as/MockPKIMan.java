/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.PKIManagement;
import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;

public class MockPKIMan implements PKIManagement
{

	@Override
	public Set<String> getCredentialNames() throws EngineException
	{
		return Collections.singleton("MAIN");
	}

	@Override
	public X509Credential getCredential(String name) throws EngineException
	{
		try
		{
			return new KeystoreCredential("src/test/resources/demoKeystore.p12", 
					"the!uvos".toCharArray(), "the!uvos".toCharArray(), null, "pkcs12");
		} catch (Exception e)
		{
			throw new InternalException("ups", e);
		}
	}

	@Override
	public Set<String> getValidatorNames() throws EngineException
	{
		return null;
	}

	@Override
	public X509CertChainValidatorExt getValidator(String name) throws EngineException
	{
		return null;
	}

	@Override
	public IAuthnAndTrustConfiguration getMainAuthnAndTrust()
	{
		return null;
	}

	@Override
	public Set<String> getCertificateNames() throws EngineException
	{
		return null;
	}

	@Override
	public X509Certificate getCertificate(String name) throws EngineException
	{
		return null;
	}

	@Override
	public void updateCertificate(String name, X509Certificate updated)
			throws EngineException
	{
	}

	@Override
	public void removeCertificate(String name) throws EngineException
	{
	}

	@Override
	public void addCertificate(String name, X509Certificate updated)
			throws EngineException
	{
	}
}