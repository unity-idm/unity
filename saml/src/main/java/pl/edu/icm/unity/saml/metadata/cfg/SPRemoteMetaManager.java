/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.metadata.cfg.MetadataVerificator.MetadataValidationException;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the retrieval, loading and update of runtime configuration based on the remote SAML metadata. 
 * @author K. Benedyczak
 */
public class SPRemoteMetaManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SPRemoteMetaManager.class);
	private final PKIManagement pkiManagement;
	private final MetadataToSPConfigConverter converter;
	private final MetadataVerificator verificator;
	private final RemoteMetadataService metadataService;
	private final Map<String, MetadataConsumer> registeredConsumers = new HashMap<>();
	private TrustedIdPs combinedTrustedIdPs;
	private SAMLSPConfiguration configuration;
	private AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader;

	private SPRemoteMetaManager(
			PKIManagement pkiManagement,
			MetadataToSPConfigConverter converter,
			RemoteMetadataService metadataService,
			AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader)
	{
		this.converter = converter;
		this.metadataService = metadataService;
		this.verificator = new MetadataVerificator();
		this.pkiManagement = pkiManagement;
		this.asyncExternalLogoFileDownloader = asyncExternalLogoFileDownloader;
	}

	public synchronized TrustedIdPs getTrustedIdPs()
	{
		return combinedTrustedIdPs;
	}
	
	public synchronized void setBaseConfiguration(SAMLSPConfiguration configuration)
	{
		if (this.configuration == null)
		{
			this.configuration = configuration;
			reinitialize(configuration);
			return;
		}
		
		Map<String, RemoteMetadataSource> oldMetaSrcs = this.configuration.trustedMetadataSourcesByUrl;
		Map<String, RemoteMetadataSource> newMetaSrcs = configuration.trustedMetadataSourcesByUrl;
		TrustedIdPs oldIndividualTrustedIdPs = this.configuration.individualTrustedIdPs;
		TrustedIdPs newIndividualTrustedIdPs = configuration.individualTrustedIdPs;
		
		boolean reload = !oldMetaSrcs.equals(newMetaSrcs) || !oldIndividualTrustedIdPs.equals(newIndividualTrustedIdPs);
		this.configuration = configuration;
		if (reload)
			reinitialize(configuration);
	}

	private void reinitialize(SAMLSPConfiguration configuration)
	{
		combinedTrustedIdPs = configuration.individualTrustedIdPs;
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
					consumer::onUpdatedMetadata);
		}
	}

	public synchronized void unregisterAll()
	{
		log.trace("Unregistering all remote metadata consumers");
		registeredConsumers.keySet().forEach(id -> metadataService.unregisterConsumer(id));
		registeredConsumers.clear();
	}
	
	private synchronized void assembleCombinedConfiguration(TrustedIdPs idpsFromMeta, String federationId, String consumerId)
	{
		if (!registeredConsumers.containsKey(consumerId)) 
			//not likely but can happen in case of race condition between 
			//deregistration of a consumer happening at the same time as async refresh
			return;
		checkDuplicatedFederations(federationId, consumerId);
		TrustedIdPs withUpdatedFederation = combinedTrustedIdPs.replaceFederation(idpsFromMeta, federationId);
		combinedTrustedIdPs = withUpdatedFederation.overrideIdPs(configuration.individualTrustedIdPs);
	}

	private void checkDuplicatedFederations(String federationId, String consumerId)
	{
		registeredConsumers.forEach((id, consumer) -> 
		{
			if (!consumerId.equals(id) && federationId.equals(consumer.federationId))
				log.error("The federation {} is configured with two metadata sources. "
						+ "This won't work, results will be unpredictible.", federationId);
		});
	}

	private TrustedIdPs parseMetadata(EntitiesDescriptorDocument metadata, RemoteMetadataSource metadataConfig)
	{
		TrustedIdPs trustedIdPs = converter.convertToTrustedIdPs(metadata, metadataConfig);
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
		private String federationId;
		
		public MetadataConsumer(RemoteMetadataSource metadataConfig)
		{
			this.metadataConfig = metadataConfig;
		}
		
		private void onUpdatedMetadata(EntitiesDescriptorDocument metadata, String consumerId)
		{
			if (!isMetadataValid(metadata, metadataConfig))
				return;
			TrustedIdPs idpsFromMeta = parseMetadata(metadata, metadataConfig);
			String federationId = metadata.getEntitiesDescriptor().getID();
			if (this.federationId == null)
				this.federationId = federationId;
			else if (!this.federationId.equals(federationId))
				throw new IllegalStateException("Consumer got metadata from different federation than before. "
						+ "Was " + this.federationId + " now it is " + federationId); 
			assembleCombinedConfiguration(idpsFromMeta, federationId, consumerId);
			asyncExternalLogoFileDownloader.downloadLogoFilesAsync(idpsFromMeta, metadataConfig.httpsTruststore);
		}
	}
	
	@Component
	public static class Factory
	{
		private final PKIManagement pkiManagement;
		private final MetadataToSPConfigConverter converter;
		private final RemoteMetadataService metadataService;
		private final AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader;

		Factory(@Qualifier("insecure") PKIManagement pkiManagement,
				MetadataToSPConfigConverter converter,
				RemoteMetadataService metadataService,
				AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader)
		{
			this.pkiManagement = pkiManagement;
			this.converter = converter;
			this.metadataService = metadataService;
			this.asyncExternalLogoFileDownloader = asyncExternalLogoFileDownloader;
		}

		public SPRemoteMetaManager getInstance()
		{
			return new SPRemoteMetaManager(pkiManagement, converter, metadataService, asyncExternalLogoFileDownloader);
		}
	}
}
