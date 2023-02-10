/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.metadata.cfg.AsyncExternalLogoFileDownloader;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * See {@link RemoteMetadataService}  
 * @author K. Benedyczak
 */
@Component
class RemoteMetadataServiceImpl implements RemoteMetadataService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML,
			RemoteMetadataServiceImpl.class);
	private final ExecutorsService executorsService;
	private final CachedMetadataLoader downloader;

	private final AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader;

	private long nextConsumerId = 0;
	private Map<String, MetadataSourceHandler> metadataHandlersByURL = new HashMap<>();
	private Map<String, String> consumers2URL = new HashMap<>();	
	
	@Autowired
	public RemoteMetadataServiceImpl(FileStorageService fileStorageService, URIAccessService uriAccessService, 
			ExecutorsService executorsService, AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader)
	{
		this.executorsService = executorsService;
		this.asyncExternalLogoFileDownloader = asyncExternalLogoFileDownloader;
		this.downloader = new CachedMetadataLoader(uriAccessService, fileStorageService);
	}

	RemoteMetadataServiceImpl(ExecutorsService executorsService, CachedMetadataLoader downloader,
	                          AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader)
	{
		this.executorsService = executorsService;
		this.downloader = downloader;
		this.asyncExternalLogoFileDownloader = asyncExternalLogoFileDownloader;
	}
	

	@Override
	public synchronized String preregisterConsumer(String url)
	{
		String key = String.valueOf(nextConsumerId++);
		consumers2URL.put(key, url);
		return key;
	}
	
	@Override
	public synchronized void registerConsumer(String key, Duration refreshInterval,
			String customTruststore, BiConsumer<EntitiesDescriptorDocument, String> consumer, boolean logoDownload)
	{
		String url = consumers2URL.get(key);
		if (url == null)
			throw new IllegalArgumentException("Key " + key + " is unknown");
		MetadataSourceHandler handler = metadataHandlersByURL.get(url);
		if (handler == null)
		{
			RemoteMetadataSrc remoteMetaDesc = new RemoteMetadataSrc(url, customTruststore);
			handler = new MetadataSourceHandler(remoteMetaDesc, executorsService, 
					downloader, asyncExternalLogoFileDownloader);
			metadataHandlersByURL.put(url, handler);
		}
		checkTruststoresConsistency(handler, customTruststore);
		handler.addConsumer(new MetadataConsumer(refreshInterval, consumer, key, logoDownload));
		log.info("Registered consumer {} of metadata from {}", key, url);
	}

	private void checkTruststoresConsistency(MetadataSourceHandler handler, String addedTruststore)
	{
		String current = handler.getSource().truststore;
		current = current == null ? "DEFAULT" : current;
		addedTruststore = addedTruststore == null ? "DEFAULT" : addedTruststore;
		if (!current.equals(addedTruststore))
			log.warn("Metadata to be downloaded from URL {} is configured to use two "
					+ "truststores. Will use {}, {} will be ignored",
					handler.getSource().url, current, addedTruststore);
	}
	
	@Override
	public synchronized void unregisterConsumer(String id)
	{
		String url = consumers2URL.remove(id);
		if (url == null)
			return;
		MetadataSourceHandler handler = metadataHandlersByURL.get(url);

		handler.removeConsumer(id);
		log.info("Unregistered consumer {} of metadata from {}", id, url);
	}

	@Override
	public synchronized void reset()
	{
		Set<String> ids = new HashSet<>(consumers2URL.keySet());
		for (String id : ids)
			unregisterConsumer(id);
		nextConsumerId = 0;
	}
}
