/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Downloads remote metadata, stores it in a local filesystem (caching in workspace). The cached file is loaded
 * and returned. In case when metadata source is a local file then the file is simply loaded and returned.
 * @author K. Benedyczak
 */
public class RemoteMetadataProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, RemoteMetadataProvider.class);
	public static final String CACHE_DIR = "downloadedMetadata";
	private PKIManagement pkiManagement;
	private UnityServerConfiguration mainConfig;
	
	public RemoteMetadataProvider(PKIManagement pkiManagement,
			UnityServerConfiguration mainConfig)
	{
		this.pkiManagement = pkiManagement;
		this.mainConfig = mainConfig;
	}

	public EntitiesDescriptorDocument load(String url, int refreshInterval) throws XmlException, IOException, EngineException
	{
		return EntitiesDescriptorDocument.Factory.parse(getAsLocalFile(url, refreshInterval));
	}
	
	private InputStream getAsLocalFile(String url, int refreshInterval) throws IOException, EngineException
	{
		if (url.startsWith("file:"))
		{
			URL localUrl = new URL(url);
			return localUrl.openStream();
		}
		
		try
		{
			tryDownloading(url, refreshInterval);
		} catch (Exception e)
		{
			log.warn("Download of remote metadata from " + url + 
					" failed, will try to load it from cache", e);
		}
		
		File cachedFile = getLocalFile(url, "");
		if (!cachedFile.exists())
		{
			throw new IOException("No cached representation of metadata for " + url + 
					" found, giving up for now.");
		}
		return new BufferedInputStream(new FileInputStream(cachedFile));
	}

	private void tryDownloading(String url, int refreshInterval) throws EngineException, IOException
	{
		File cachedFilePart = getLocalFile(url, "_part");
		File cachedFile = getLocalFile(url, "");
		if (cachedFilePart.exists())
			cachedFilePart.delete();
		
		long expirationTime = System.currentTimeMillis() - (refreshInterval*100);
		if (cachedFile.exists() && cachedFile.lastModified() < expirationTime)
		{
			log.trace("Locally cached metadata file is fresh, skipping downloading " + cachedFile);
			return;
		}
		log.debug("Downloading metadata from " + url + " to " + cachedFilePart.toString());
		HttpClient client = url.startsWith("https:") ? getSSLClient(url) : new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		{
			String body = response.getEntity().getContentLength() < 10240 ? 
					EntityUtils.toString(response.getEntity()) : "";
			throw new IOException("Metadata download from " + url + " error: " + 
					response.getStatusLine().toString() + "; " + body);
		}

		InputStream is = response.getEntity().getContent();
		FileOutputStream cacheFos = new FileOutputStream(cachedFilePart);
		IOUtils.copy(is, cacheFos);
		cacheFos.close();

		cachedFile.delete();
		FileUtils.moveFile(cachedFilePart, cachedFile);
		log.debug("Downloaded metadata from " + url + " to final file " + cachedFile.toString());
	}
	
	private File getLocalFile(String uri, String suffix)
	{
		File dir = new File(mainConfig.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY), CACHE_DIR);
		if (!dir.exists())
			dir.mkdirs();
		File ret = new File(dir, DigestUtils.md5Hex(uri) + suffix);
		return ret;
	}
	
	private HttpClient getSSLClient(String url) throws EngineException
	{
		DefaultClientConfiguration config = new DefaultClientConfiguration();
		config.setSslEnabled(true);
		config.setValidator(pkiManagement.getValidator(
				mainConfig.getValue(UnityServerConfiguration.MAIN_TRUSTSTORE)));
		return HttpUtils.createClient(url, config);
	}
}
