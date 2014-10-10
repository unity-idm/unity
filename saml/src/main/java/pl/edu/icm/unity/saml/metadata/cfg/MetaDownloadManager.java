/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;

/**
 * Downloads remote metadata, stores it in a local filesystem (caching in
 * workspace).
 * 
 * @author P. Piernik
 */

public class MetaDownloadManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML,
			MetaDownloadManager.class);

	public static final String CACHE_DIR = "downloadedMetadata";
	private PKIManagement pkiManagement;
	private UnityServerConfiguration mainConfig;
	private Map<String, Long> downloadedFiles;
	private Set<String> downlodingFiles;

	@Autowired
	public MetaDownloadManager(PKIManagement pkiManagement, UnityServerConfiguration mainConfig)
	{
		this.pkiManagement = pkiManagement;
		this.mainConfig = mainConfig;
		this.downloadedFiles = new HashMap<String, Long>();
		this.downlodingFiles = new HashSet<String>();

	}

	public void waitToDownload(String url) throws InterruptedException
	{	
		while (downlodingFiles.contains(url))
		{
			Thread.sleep(5000);
		}	
	}

	public synchronized int checkWaiting(String url)
	{
		if (downlodingFiles.contains(url))
		{
			return 1;
		}

		downlodingFiles.add(url);
		return 0;
	}

	public File tryDownloading(String url, int refreshInterval, String customTruststore)
			throws EngineException, IOException
	{

		Long lastDownload = downloadedFiles.get(url);
		if (lastDownload != null)
		{
			long expirationTime = System.currentTimeMillis() - (refreshInterval * 100l);
			if (lastDownload < expirationTime)
			{
				log.trace("Locally cached metadata file is fresh, skipping downloading " + url);
				return getFromCache(url, "");
			}
		}

		if (checkWaiting(url) == 1)
		{
			log.trace("Another process download file from " + url
					+ ", waiting to complete download");
			try
			{
				waitToDownload(url);
			} catch (InterruptedException e)
			{
				log.error("Error in waiting for the end downloading file " + url, e);
			}
			return getFromCache(url, "");
		}

		try
		{
			download(url, customTruststore);
		} catch (Exception ex)
		{
			downlodingFiles.remove(url);
			throw ex;
		}

		File cachedFile = getFromCache(url, "");
		downloadedFiles.put(url, cachedFile.lastModified());
		downlodingFiles.remove(url);

		return cachedFile;
	}

	private void download(String url, String customTruststore) throws EngineException,
			IOException
	{
		File cachedFile = getLocalFile(url, "");
		File cachedFilePart = getLocalFile(url, "_part");
		if (cachedFilePart.exists())
			cachedFilePart.delete();

		log.debug("Downloading metadata from " + url + " to " + cachedFilePart.toString());
		HttpClient client = url.startsWith("https:") ? getSSLClient(url, customTruststore)
				: new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		HttpResponse response = client.execute(request);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		{
			String body = response.getEntity().getContentLength() < 10240 ? EntityUtils
					.toString(response.getEntity()) : "";

			throw new IOException("Metadata download from " + url + " error: "
					+ response.getStatusLine().toString() + "; " + body);

		}

		InputStream is = response.getEntity().getContent();
		FileOutputStream cacheFos = new FileOutputStream(cachedFilePart);
		IOUtils.copy(is, cacheFos);
		cacheFos.close();

		cachedFile.delete();
		FileUtils.moveFile(cachedFilePart, cachedFile);
		log.debug("Downloaded metadata from " + url + " to final file "
				+ cachedFile.toString());
	}

	public File getFromCache(String uri, String suffix) throws IOException
	{
		File resp = getLocalFile(uri, suffix);
		if (!resp.exists())
		{
			throw new IOException("No cached representation of metadata for " + uri);
		}
		return resp;

	}

	private File getLocalFile(String uri, String suffix)
	{
		File dir = new File(
				mainConfig.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY),
				CACHE_DIR);
		if (!dir.exists())
			dir.mkdirs();
		File ret = new File(dir, DigestUtils.md5Hex(uri) + suffix);
		return ret;
	}

	private HttpClient getSSLClient(String url, String customTruststore) throws EngineException
	{
		if (customTruststore != null)
		{
			DefaultClientConfiguration config = new DefaultClientConfiguration();
			config.setSslEnabled(true);
			config.setValidator(pkiManagement.getValidator(customTruststore));
			return HttpUtils.createClient(url, config);
		} else
		{
			return new DefaultHttpClient();
		}
	}
}
