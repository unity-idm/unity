/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.CertificateUtils.Encoding;
import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.DefaultAuthnAndTrustConfiguration;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import eu.unicore.security.canl.LoggingStoreUpdateListener;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.UnityPKIConfiguration;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

/**
 * Implementation of {@link PKIManagement}. Currently pretty simplistic: all artifacts are resolved 
 * wrt the configuration loaded from disk.
 * @author K. Benedyczak
 */
@Component
public class PKIManagementImpl implements PKIManagement
{
	private UnityPKIConfiguration pkiConf;
	private Map<String, X509Credential> credentials;
	private Map<String, X509CertChainValidatorExt> validators;
	private Map<String, X509Certificate> certificates;
	private IAuthnAndTrustConfiguration mainAuthnTrust;
	
	@Autowired
	public PKIManagementImpl(UnityServerConfiguration mainConf)
	{
		this.pkiConf = mainConf.getPKIConfiguration();
		Set<String> credNames = pkiConf.getStructuredListKeys(UnityPKIConfiguration.CREDENTIALS);
		credentials = new HashMap<String, X509Credential>();
		for (String cred: credNames)
		{
			CredentialProperties tmp = new CredentialProperties(pkiConf.getProperties(), 
					pkiConf.getCredentialPrefix(cred));
			credentials.put(pkiConf.getCredentialName(cred), tmp.getCredential());
		}

		Set<String> valNames = pkiConf.getStructuredListKeys(UnityPKIConfiguration.TRUSTSTORES);
		validators = new HashMap<>();
		for (String validator: valNames)
		{
			TruststoreProperties tmp = new TruststoreProperties(pkiConf.getProperties(), 
					Collections.singleton(new LoggingStoreUpdateListener()),
					pkiConf.getTruststorePrefix(validator));
			validators.put(pkiConf.getTruststoreName(validator), tmp.getValidator());
		}
		
		try
		{
			mainAuthnTrust = new DefaultAuthnAndTrustConfiguration(
					getValidator(mainConf.getValue(UnityServerConfiguration.MAIN_TRUSTSTORE)), 
					getCredential(mainConf.getValue(UnityServerConfiguration.MAIN_CREDENTIAL)));
		} catch (EngineException e)
		{
			throw new ConfigurationException("Can't load the main server credential/truststore", e);
		}
		
		Set<String> certNames = pkiConf.getStructuredListKeys(UnityPKIConfiguration.CERTIFICATES);
		certificates = new HashMap<String, X509Certificate>();
		for (String cert: certNames)
		{
			FileInputStream fis;
			try
			{
				fis = new FileInputStream(pkiConf.getFileValue(
						cert+UnityPKIConfiguration.CERTIFICATE_FILE, false));
				X509Certificate certificate = CertificateUtils.loadCertificate(fis, Encoding.PEM);
				certificates.put(pkiConf.getCertificateName(cert), certificate);
			} catch (IOException e)
			{
				throw new ConfigurationException("Can not load certificate " + cert, e);
			}
		}
	}
	
	
	@Override
	public Set<String> getCredentialNames() throws EngineException
	{
		return credentials.keySet();
	}

	@Override
	public X509Credential getCredential(String name) throws EngineException
	{
		if (!getCredentialNames().contains(name))
			throw new WrongArgumentException("The credential " + name + " is not defined. " +
					"Available credentials: " + getCredentialNames());
		return credentials.get(name);
	}

	@Override
	public Set<String> getValidatorNames() throws EngineException
	{
		return validators.keySet();
	}

	@Override
	public X509CertChainValidatorExt getValidator(String name) throws EngineException
	{
		if (!getValidatorNames().contains(name))
			throw new WrongArgumentException("The truststore " + name + " is not defined. " +
					"Available truststores: " + getValidatorNames());
		return validators.get(name);
	}


	@Override
	public IAuthnAndTrustConfiguration getMainAuthnAndTrust()
	{
		return mainAuthnTrust;
	}


	@Override
	public Set<String> getCertificateNames() throws EngineException
	{
		return certificates.keySet();
	}


	@Override
	public X509Certificate getCertificate(String name) throws EngineException
	{
		if (!getCertificateNames().contains(name))
			throw new WrongArgumentException("The certificate " + name + " is not defined. " +
					"Available certificates: " + getCertificateNames());
		return certificates.get(name);
	}
}
