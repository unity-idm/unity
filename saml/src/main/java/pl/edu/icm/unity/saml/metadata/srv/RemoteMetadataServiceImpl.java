/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Handles registration of metadata consumers and manages workers handling 
 * individual metadata retrievals.
 *  
 * @author K. Benedyczak
 */
@Component
class RemoteMetadataServiceImpl implements RemoteMetadataService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML,
			RemoteMetadataServiceImpl.class);
	private final ExecutorsService executorsService;
	private final MetadataDownloader downloader;

	private long nextConsumerId = 0;
	private Map<String, MetadataSourceHandler> metadataHandlersByURL = new HashMap<>();
	private Map<String, String> consumers2URL = new HashMap<>();	
	
	@Autowired
	public RemoteMetadataServiceImpl(ExecutorsService executorsService, PKIManagement pkiManagement,
			UnityServerConfiguration config)
	{
		this.executorsService = executorsService;
		NetworkClient client = new NetworkClient(pkiManagement);
		String workspaceDirectory = config.getValue(
				UnityServerConfiguration.WORKSPACE_DIRECTORY);
		this.downloader = new MetadataDownloader(workspaceDirectory, client);
	}

	RemoteMetadataServiceImpl(ExecutorsService executorsService, MetadataDownloader downloader)
	{
		this.executorsService = executorsService;
		this.downloader = downloader;
	}
	
	@Override
	public synchronized String registerConsumer(String url, long refreshIntervalMs,
			String customTruststore, Consumer<EntitiesDescriptorDocument> consumer)
	{
		MetadataSourceHandler handler = metadataHandlersByURL.get(url);
		if (handler == null)
		{
			RemoteMetadataSrc remoteMetaDesc = new RemoteMetadataSrc(url, customTruststore);
			handler = new MetadataSourceHandler(remoteMetaDesc, executorsService, 
					downloader);
			metadataHandlersByURL.put(url, handler);
		}
		checkTruststoresConsistency(handler, customTruststore);
		String key = String.valueOf(nextConsumerId++);
		handler.addConsumer(new MetadataConsumer(refreshIntervalMs, consumer, key));
		consumers2URL.put(String.valueOf(key), url);
		log.info("Registered consumer {} of metadata from {}", key, url);
		return key;
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

		if (handler.removeConsumer(id))
		{
			metadataHandlersByURL.remove(url);
			log.debug("Unregistered the last consumer, handler is removed for {}", url);
		}
		log.info("Unregistered consumer {} of metadata from {}", id, url);
	}
}
