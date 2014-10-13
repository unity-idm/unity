/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.xmlbeans.XmlException;

import pl.edu.icm.unity.exceptions.EngineException;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Load and return remote meta files. In case when metadata source is a local file then the file is simply loaded and returned.
 * @author K. Benedyczak
 */
public class RemoteMetadataProvider
{
	private MetaDownloadManager downloadManager;
	
	public RemoteMetadataProvider(MetaDownloadManager downloadManager)
	{
		this.downloadManager = downloadManager;
	}

	public EntitiesDescriptorDocument load(String url, int refreshInterval,
			String customTruststore) throws XmlException, IOException, EngineException
	{
		return EntitiesDescriptorDocument.Factory.parse(getAsLocalFile(url, refreshInterval, customTruststore));
	}
	/**
	 * If url is local file return its stream in other case try download remote file using download manager
	 * @param url
	 * @param refreshInterval
	 * @param customTruststore
	 * @return
	 * @throws IOException
	 * @throws EngineException
	 */
	private InputStream getAsLocalFile(String url, int refreshInterval,
			String customTruststore) throws IOException, EngineException
	{
		if (url.startsWith("file:"))
		{
			URL localUrl = new URL(url);
			return localUrl.openStream();
		}
			
		File cachedFile = downloadManager.tryDownloading(url, refreshInterval, customTruststore);
		return new BufferedInputStream(new FileInputStream(cachedFile));
	}	
}
