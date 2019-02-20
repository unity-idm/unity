/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
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
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityPKIConfiguration;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.CertificateDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredCertificate;

/**
 * Implementation of {@link PKIManagement}. Currently pretty simplistic: all
 * artifacts are resolved wrt the configuration loaded from disk.
 * <p>
 * The certificates can be also set via API as persistend - stored in db
 * or as volatile - stored only in memory.
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
public class PKIManagementImpl implements PKIManagement
{
	private InternalAuthorizationManager authz;
	private UnityPKIConfiguration pkiConf;
	private Map<String, X509Credential> credentials;
	private Map<String, X509CertChainValidatorExt> validators;
	private Map<String, NamedCertificate> certificates;
	private IAuthnAndTrustConfiguration mainAuthnTrust;
	private CertificateDB certDB;

	@Autowired
	public PKIManagementImpl(UnityServerConfiguration mainConf, CertificateDB certDB, InternalAuthorizationManager authz)
	{
		this.pkiConf = mainConf.getPKIConfiguration();
		this.certDB = certDB;
		this.authz = authz;

		Set<String> credNames = pkiConf.getStructuredListKeys(UnityPKIConfiguration.CREDENTIALS);
		credentials = new HashMap<String, X509Credential>();
		for (String cred : credNames)
		{
			CredentialProperties tmp = new CredentialProperties(pkiConf.getProperties(),
					pkiConf.getCredentialPrefix(cred));
			credentials.put(pkiConf.getCredentialName(cred), tmp.getCredential());
		}

		Set<String> valNames = pkiConf.getStructuredListKeys(UnityPKIConfiguration.TRUSTSTORES);
		validators = new HashMap<>();
		for (String validator : valNames)
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

		certificates = new HashMap<>();
	}

	@Transactional
	@Override
	public void loadCertificatesFromConfigFile()
	{

		Set<String> allInDB = certDB.getAllNames();
		Set<String> certNames = pkiConf.getStructuredListKeys(UnityPKIConfiguration.CERTIFICATES);
		for (String cert : certNames)
		{
			String certName = pkiConf.getCertificateName(cert);
			try
			{
				FileInputStream fis = new FileInputStream(pkiConf
						.getFileValue(cert + UnityPKIConfiguration.CERTIFICATE_FILE, false));
				X509Certificate x509Cert = getX509Certificate(fis);
				NamedCertificate unityCert = new NamedCertificate(certName, x509Cert);
				StoredCertificate storedCert = toStoredCert(unityCert);
				if (allInDB.contains(certName))
				{
					certDB.update(storedCert);
				} else
				{
					certDB.create(storedCert);
				}

			} catch (IOException e)
			{

				throw new ConfigurationException("Can not load certificate " + certName, e);
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
			throw new WrongArgumentException("The credential " + name + " is not defined. "
					+ "Available credentials: " + getCredentialNames());
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
			throw new WrongArgumentException("The truststore " + name + " is not defined. "
					+ "Available truststores: " + getValidatorNames());
		return validators.get(name);
	}

	@Override
	public IAuthnAndTrustConfiguration getMainAuthnAndTrust()
	{
		return mainAuthnTrust;
	}

	@Transactional
	@Override
	public synchronized Set<String> getAllCertificateNames() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		Set<String> allNames = new HashSet<>();
		allNames.addAll(certDB.getAllNames());
		allNames.addAll(certificates.keySet());
		return allNames;
	}

	@Transactional
	@Override
	public synchronized NamedCertificate getCertificate(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		NamedCertificate cert = certificates.get(name);
		if (cert == null)
		{ 
			cert = fromStoredCert(certDB.get(name));	
		}
		return cert;
	}
	
	@Override
	public synchronized List<NamedCertificate> getVolatileCertificates() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return new ArrayList<>(certificates.values());
	}

	@Transactional
	@Override
	public synchronized void addVolatileCertificate(String name, X509Certificate updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertCertificateExists(name);
		certificates.put(name, new NamedCertificate(name, updated));
	}

	@Transactional
	@Override
	public synchronized void addPersistedCertificate(NamedCertificate toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertCertificateExists(toAdd.name);
		certDB.create(toStoredCert(toAdd));
	}

	@Transactional
	@Override
	public List<NamedCertificate> getPersistedCertificates() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return certDB.getAll().stream().map(this::fromStoredCert).collect(Collectors.toList());
	}

	@Transactional
	@Override
	public synchronized void removeCertificate(String toRemove) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		if (certificates.remove(toRemove) == null)
		{
			certDB.delete(toRemove);
		}
	}

	@Transactional
	@Override
	public synchronized void updateCertificate(NamedCertificate toUpdate) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		if (certificates.containsKey(toUpdate.name))
		{
			certificates.put(toUpdate.name, toUpdate);
			return;
		}
		certDB.update(toStoredCert(toUpdate));
	}

	void assertCertificateExists(String name)
	{
		if (!certificates.containsKey(name))
		{
			try
			{
				certDB.get(name);
			} catch (IllegalArgumentException e)
			{
				//ok
				return;
			}
		}

		throw new IllegalArgumentException("The certificate labelled " + name + " already exists");
	}	
	
	private StoredCertificate toStoredCert(NamedCertificate cert)
	{
		return new StoredCertificate(cert.name, getPemStringFromCert(cert.value));
	}

	private NamedCertificate fromStoredCert(StoredCertificate cert)
	{
		return new NamedCertificate(cert.getName(),
				getX509Certificate(new ByteArrayInputStream(cert.getValue().getBytes())));
	}

	private X509Certificate getX509Certificate(InputStream in)
	{
		try
		{
			return CertificateUtils.loadCertificate(in, Encoding.PEM);

		} catch (IOException e)
		{
			throw new InternalException("Can not load certificate from string", e);
		}
	}

	private String getPemStringFromCert(X509Certificate cert)
	{
		try
		{
			OutputStream out = new ByteArrayOutputStream();
			CertificateUtils.saveCertificate(out, cert, Encoding.PEM);
			return new String(out.toString());
		} catch (IOException e)
		{
			throw new InternalException("Can not parse certificate to string", e);
		}
	}
}
