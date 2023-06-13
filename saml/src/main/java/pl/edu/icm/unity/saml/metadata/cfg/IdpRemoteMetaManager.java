/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.metadata.cfg.MetadataVerificator.MetadataValidationException;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.idp.TrustedServiceProvider;
import pl.edu.icm.unity.saml.idp.TrustedServiceProviders;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;

/**
 * Manages the retrieval, loading and update of runtime configuration based on the remote SAML metadata.
 */
public class IdpRemoteMetaManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, IdpRemoteMetaManager.class);
	private final PKIManagement pkiManagement;
	private final MetaToIDPConfigConverter converter;
	private final MetadataVerificator verificator;
	private final RemoteMetadataService metadataService;
	private final Map<String, MetadataConsumer> registeredConsumers = new HashMap<>();
	private SAMLIdPConfiguration configuration;
	
	public IdpRemoteMetaManager(SAMLIdPConfiguration configuration, PKIManagement pkiManagement,
	                            RemoteMetadataService metadataService, MetaToIDPConfigConverter converter)
	{
		this.metadataService = metadataService;
		this.verificator = new MetadataVerificator();
		this.pkiManagement = pkiManagement;
		this.converter = converter;
		setBaseConfiguration(configuration);
	}

	public synchronized TrustedServiceProviders getTrustedSps()
	{
		return configuration.trustedServiceProviders;
	}

	public SAMLIdPConfiguration getSAMLIdPConfiguration()
	{
		return configuration;
	}

	synchronized void setBaseConfiguration(SAMLIdPConfiguration configuration)
	{
		if (this.configuration == null)
		{
			this.configuration = configuration;
			reinitialize();
			return;
		}

		Map<String, RemoteMetadataSource> oldMetaSrcs = this.configuration.trustedMetadataSourcesByUrl;
		Map<String, RemoteMetadataSource> newMetaSrcs = configuration.trustedMetadataSourcesByUrl;
		TrustedServiceProviders oldIndividualTrustedIdPs = this.configuration.trustedServiceProviders;
		TrustedServiceProviders newIndividualTrustedIdPs = configuration.trustedServiceProviders;

		boolean reload = !oldMetaSrcs.equals(newMetaSrcs) || !oldIndividualTrustedIdPs.equals(newIndividualTrustedIdPs);
		this.configuration = configuration;
		if (reload)
			reinitialize();
	}

	private void reinitialize()
	{
		unregisterAll();
		registerMetadataConsumers();
	}

	private void registerMetadataConsumers()
	{
		log.trace("Registering remote metadata consumers");
		for (RemoteMetadataSource metadataSource: configuration.trustedMetadataSourcesByUrl.values())
		{
			String url = metadataSource.url;
			Duration refreshInterval = metadataSource.refreshInterval;
			String customTruststore = metadataSource.httpsTruststore;
			MetadataConsumer consumer = new MetadataConsumer(metadataSource);
			String consumerId = metadataService.preregisterConsumer(url);
			registeredConsumers.put(consumerId, consumer);
			metadataService.registerConsumer(consumerId, refreshInterval, customTruststore,
					consumer::updateMetadata, false);
		}
	}

	public synchronized void unregisterAll()
	{
		log.trace("Unregistering all remote metadata consumers");
		registeredConsumers.keySet().forEach(metadataService::unregisterConsumer);
		registeredConsumers.clear();
	}

	private synchronized void assembleCombinedConfiguration(Set<TrustedServiceProvider> trustedIdPs, String consumerId)
	{
		if (!registeredConsumers.containsKey(consumerId))
			//not likely but can happen in case of race condition between
			//deregistration of a consumer happening at the same time as async refresh
			return;
		configuration.trustedServiceProviders.replace(trustedIdPs);
		configuration.load();
	}

	private Set<TrustedServiceProvider> parseMetadata(EntitiesDescriptorDocument metadata, RemoteMetadataSource metadataConfig)
	{
		Set<TrustedServiceProvider> trustedIdPs = converter.convertToTrustedSps(metadata, configuration);
		log.trace("Converted metadata from {} to virtual configuration", metadataConfig.url);
		return trustedIdPs;
	}

	private boolean isMetadataValid(EntitiesDescriptorDocument metadata, RemoteMetadataSource metadataConfig)
	{
		String issuerCertificateName = metadataConfig.issuerCertificate;
		try
		{
			X509Certificate issuerCertificate = issuerCertificateName != null ?
					pkiManagement.getCertificate(issuerCertificateName).value: null;
			verificator.validate(metadata, new Date(), metadataConfig.signatureValidation, issuerCertificate);
			return true;
		} catch (MetadataValidationException e)
		{
			log.error("Metadata from " + metadataConfig.url + " is invalid, won't be used", e);
			return false;
		} catch (EngineException e)
		{
			log.error("Problem establishing certificate for metadata validation " +
					issuerCertificateName, e);
			return false;
		}
	}

	private class MetadataConsumer
	{
		private final RemoteMetadataSource metadataConfig;

		public MetadataConsumer(RemoteMetadataSource metadataConfig)
		{
			this.metadataConfig = metadataConfig;
		}
		
		private void updateMetadata(EntitiesDescriptorDocument metadata, String consumerId)
		{
			if (!isMetadataValid(metadata, metadataConfig))
				return;
			Set<TrustedServiceProvider> trustedIdPs = parseMetadata(metadata, metadataConfig);
			assembleCombinedConfiguration(trustedIdPs, consumerId);
		}
	}
}
