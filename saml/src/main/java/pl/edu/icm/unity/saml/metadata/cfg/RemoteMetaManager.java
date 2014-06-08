/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.metadata.cfg.MetadataVerificator.MetadataValidationException;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Manages the retrieval, loading and update of runtime configuration based on the remote SAML metadata. 
 * @author K. Benedyczak
 */
public class RemoteMetaManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, RemoteMetaManager.class);
	private PKIManagement pkiManagement;
	private SAMLSPProperties configuration;
	private ExecutorsService executorsService;
	private RemoteMetadataProvider remoteMetaProvider;
	private MetaToSPConfigConverter converter;
	private MetadataVerificator verificator;
	private SAMLSPProperties virtualConfiguration;
	private Date validationDate;
	
	public RemoteMetaManager(SAMLSPProperties configuration, UnityServerConfiguration mainConfig,
			ExecutorsService executorsService, PKIManagement pkiManagement)
	{
		this.configuration = configuration;
		this.executorsService = executorsService;
		this.converter = new MetaToSPConfigConverter(pkiManagement);
		this.remoteMetaProvider = new RemoteMetadataProvider(pkiManagement, mainConfig);
		this.verificator = new MetadataVerificator();
		this.pkiManagement = pkiManagement;
		this.virtualConfiguration = configuration.clone();
	}

	public void start()
	{
		long delay = configuration.getLongValue(SAMLSPProperties.IDPMETA_REFRESH);
		executorsService.getService().scheduleWithFixedDelay(new Runnable()
		{
			public void run()
			{
				try
				{
					reloadAll();
				} catch (Exception e)
				{
					log.error("Problem loading metadata of external IdP(s)", e);
				}
			}
		}, 0, delay, TimeUnit.SECONDS);
	}
	
	public void reloadAll()
	{
		Set<String> keys = configuration.getStructuredListKeys(SAMLSPProperties.IDPMETA_PREFIX);
		if (keys.isEmpty())
			return;
		Properties virtualConfigProps = configuration.getSourceProperties();
		for (String key: keys)
		{
			reloadSingle(key, virtualConfigProps);
		}
		setVirtualConfiguration(virtualConfigProps);
	}
	
	public synchronized SAMLSPProperties getVirtualConfiguration()
	{
		return virtualConfiguration;
	}

	public synchronized void setVirtualConfiguration(Properties virtualConfigurationProperties)
	{
		this.virtualConfiguration.setProperties(virtualConfigurationProperties);
	}

	private void reloadSingle(String key, Properties virtualProps)
	{
		String url = configuration.getValue(key + SAMLSPProperties.IDPMETA_URL);
		int refreshInterval = configuration.getIntValue(key + SAMLSPProperties.IDPMETA_REFRESH);
		EntitiesDescriptorDocument metadata;
		try
		{
			metadata = remoteMetaProvider.load(url, refreshInterval);
		} catch (XmlException e)
		{
			log.warn("Metadata from " + url + " was downloaded, but can not be parsed", e);
			return;
		} catch (IOException e)
		{
			log.warn("Problem fetching metadata from " + url, e);
			return;
		} catch (EngineException e)
		{
			log.error("Internal problem fetching metadata from " + url, e);
			return;
		}
		
		MetadataSignatureValidation sigCheckingMode = configuration.getEnumValue(
				key + SAMLSPProperties.IDPMETA_SIGNATURE, MetadataSignatureValidation.class);
		String issuerCertificateName = configuration.getValue(key + SAMLSPProperties.IDPMETA_ISSUER_CERT);
		
		try
		{
			X509Certificate issuerCertificate = issuerCertificateName != null ? 
					pkiManagement.getCertificate(issuerCertificateName) : null;
			verificator.validate(metadata, validationDate != null ? validationDate : new Date(),
					sigCheckingMode, issuerCertificate);
		} catch (MetadataValidationException e)
		{
			log.error("Metadata from " + url + " is invalid, won't be used", e);
			return;
		} catch (EngineException e)
		{
			log.error("Problem establishing certificate for metadata validation " + 
					issuerCertificateName, e);
			return;
		}
		
		converter.convertToProperties(metadata, virtualProps, configuration, key);
	}

	public void setValidationDate(Date validationDate)
	{
		this.validationDate = validationDate;
	}
}
