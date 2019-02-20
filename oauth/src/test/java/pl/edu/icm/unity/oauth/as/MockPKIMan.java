/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;

public class MockPKIMan implements PKIManagement
{
	private String keyPath;
	private String keyPass;
	
	public MockPKIMan()
	{
		keyPath = "src/test/resources/pki/demoKeystore.p12";
		keyPass = DBIntegrationTestBase.DEMO_KS_PASS;
				
	}
	
	public MockPKIMan(String keyPath, String keyPass)
	{
		this.keyPath = keyPath;
		this.keyPass = keyPass;
				
	}
	
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
			return new KeystoreCredential(keyPath, 
					keyPass.toCharArray(), keyPass.toCharArray(), null, "pkcs12");
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
	public Set<String> getAllCertificateNames() throws EngineException
	{
		return null;
	}

	@Override
	public NamedCertificate getCertificate(String name) throws EngineException
	{
		return null;
	}

	@Override
	public void addVolatileCertificate(String name, X509Certificate updated)
			throws EngineException
	{
	}

	@Override
	public void addPersistedCertificate(NamedCertificate toAdd) throws EngineException
	{
		
	}

	@Override
	public List<NamedCertificate> getPersistedCertificates() throws EngineException
	{
		return null;
	}

	@Override
	public void loadCertificatesFromConfigFile()
	{
		
	}

	@Override
	public List<NamedCertificate> getVolatileCertificates() throws EngineException
	{
		return null;
	}
	
	@Override
	public void removeCertificate(String toRemove) throws EngineException
	{
		
		
	}

	@Override
	public void updateCertificate(NamedCertificate toUpdate) throws EngineException
	{
		
	}
}