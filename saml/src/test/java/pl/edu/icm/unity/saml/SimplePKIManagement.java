/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.PKIManagement;
import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;

/**
 * For tests - returns only a single MAIN credential
 * @author K. Benedyczak
 */
public class SimplePKIManagement implements PKIManagement
{
	@Override
	public Set<String> getValidatorNames() throws EngineException
	{
		return new HashSet<>();
	}
	
	@Override
	public X509CertChainValidatorExt getValidator(String name) throws EngineException
	{
		throw new WrongArgumentException("No such validator " + name);
	}
	
	@Override
	public Set<String> getCredentialNames() throws EngineException
	{
		return Collections.singleton("MAIN");
	}
	@Override
	public X509Credential getCredential(String name) throws EngineException
	{
		if (name.equals("MAIN"))
			try
			{
				return new KeystoreCredential("src/test/resources/demoKeystore.p12",
						"the!uvos".toCharArray(), "the!uvos".toCharArray(), 
						null, "PKCS12");
			} catch (Exception e)
			{
				throw new InternalException("error loading credential", e);
			}
		throw new WrongArgumentException("No such validator " + name);				
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
