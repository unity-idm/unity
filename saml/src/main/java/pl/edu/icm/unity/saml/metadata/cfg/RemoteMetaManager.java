/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.metadata.cfg.MetadataVerificator.MetadataValidationException;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;
import pl.edu.icm.unity.saml.sp.web.IdPVisalSettings;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Manages the retrieval, loading and update of runtime configuration based on the remote SAML metadata. 
 * @author K. Benedyczak
 */
public class RemoteMetaManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, RemoteMetaManager.class);
	private PKIManagement pkiManagement;
	private SamlProperties configuration;
	private AbstractMetaToConfigConverter converter;
	private MetadataVerificator verificator;
	private SamlProperties virtualConfiguration;
	private String metaPrefix;
	private RemoteMetadataService metadataService;
	private Set<String> registeredConsumers = new HashSet<>();
	private Map<String, Properties> configurationsFromMetadata = new HashMap<>();
	
	public RemoteMetaManager(SamlProperties configuration, 
			PKIManagement pkiManagement,
			AbstractMetaToConfigConverter converter,
			RemoteMetadataService metadataService, String metaPrefix)
	{
		this.configuration = configuration;
		this.converter = converter;
		this.metadataService = metadataService;
		this.verificator = new MetadataVerificator();
		this.pkiManagement = pkiManagement;
		this.virtualConfiguration = configuration.clone();
		this.metaPrefix = metaPrefix;
		registerMetadataConsumers();
	}

	public synchronized SamlProperties getVirtualConfiguration()
	{
		return virtualConfiguration.clone();
	}

	public synchronized IdPVisalSettings getVisualSettings(String configKey, Locale locale)
	{
		String logoUrl = virtualConfiguration.getLocalizedValue(configKey + SAMLSPProperties.IDP_LOGO, locale);
		String name = ((SAMLSPProperties)virtualConfiguration).getLocalizedName(configKey, locale);
		List<String> tags = virtualConfiguration.getListOfValues(configKey + SAMLSPProperties.IDP_NAME + ".");
		return new IdPVisalSettings(logoUrl, tags, name);
	}
	
	public synchronized void setBaseConfiguration(SamlProperties configuration)
	{
		Properties oldP = this.configuration.getProperties();
		Properties newP = configuration.getProperties();
		boolean reload = !oldP.equals(newP);
		this.configuration = configuration;
		if (reload)
		{
			unregisterAll();
			virtualConfiguration = configuration.clone();
			registerMetadataConsumers();
		}
	}
	
	private void registerMetadataConsumers()
	{
		log.trace("Registering remote metadata consumers");
		Set<String> keys = configuration.getStructuredListKeys(metaPrefix);
		for (String key: keys)
		{
			String url = configuration.getValue(key + SamlProperties.METADATA_URL);
			long refreshInterval = configuration.getIntValue(key + SamlProperties.METADATA_REFRESH) * 1000L;
			String customTruststore = configuration.getValue(key + SamlProperties.METADATA_HTTPS_TRUSTSTORE);
			MetadataConsumer consumer = new MetadataConsumer(url, key);
			String consumerId = metadataService.preregisterConsumer(url);
			registeredConsumers.add(consumerId);
			metadataService.registerConsumer(consumerId, refreshInterval, customTruststore, 
					consumer::updateMetadata);
		}
	}

	public synchronized void unregisterAll()
	{
		log.trace("Unregistering all remote metadata consumers");
		registeredConsumers.forEach(id -> metadataService.unregisterConsumer(id));
		registeredConsumers.clear();
	}
	
	private synchronized void assembleProperties(String propertiesKey, Properties newProperties, String consumerId)
	{
		if (!registeredConsumers.contains(consumerId)) 
			//not likely but can happen in case of race condition between 
			//deregistration of a consumer happening at the same time as async refresh
			return;
		Properties virtualConfigProps = configuration.getSourceProperties();
		configurationsFromMetadata.put(propertiesKey, newProperties);
		for (Properties properties: configurationsFromMetadata.values())
			virtualConfigProps.putAll(properties);
		this.virtualConfiguration.setProperties(virtualConfigProps);
	}

	private void reloadSingle(EntitiesDescriptorDocument metadata, String key, String url,
			Properties virtualProps, SamlProperties configuration)
	{
		MetadataSignatureValidation sigCheckingMode = configuration.getEnumValue(
				key + SamlProperties.METADATA_SIGNATURE, MetadataSignatureValidation.class);
		String issuerCertificateName = configuration.getValue(key + SamlProperties.METADATA_ISSUER_CERT);
		
		try
		{
			X509Certificate issuerCertificate = issuerCertificateName != null ? 
					pkiManagement.getCertificate(issuerCertificateName).value: null;
			verificator.validate(metadata, new Date(),
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
		log.trace("Converted metadata from " + url + " to virtual configuration");
	}

	private class MetadataConsumer
	{
		private String url;
		private String propertiesKey;
		
		public MetadataConsumer(String url, String propertiesKey)
		{
			this.url = url;
			this.propertiesKey = propertiesKey;
		}
		
		private void updateMetadata(EntitiesDescriptorDocument metadata, String consumerId)
		{
			Properties virtualConfigProps = configuration.getSourceProperties();
			reloadSingle(metadata, propertiesKey, url, virtualConfigProps, configuration);
			assembleProperties(propertiesKey, virtualConfigProps, consumerId);
		}
	}
}
