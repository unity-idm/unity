/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
import pl.edu.icm.unity.exceptions.InternalException;
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
	private HashSet<String> downlodingFiles;

	@Autowired
	public MetaDownloadManager(PKIManagement pkiManagement, UnityServerConfiguration mainConfig)
	{
		this.pkiManagement = pkiManagement;
		this.mainConfig = mainConfig;
		this.downloadedFiles = new HashMap<String, Long>();
		this.downlodingFiles = new HashSet<String>();
	}
	
	/**
	 * Download or waits until another process finishes downloading the file.
	 * @param url file to download
	 * @param refreshInterval used to calculate  freshness of file
	 * @param customTruststore
	 * @return
	 * @throws EngineException
	 * @throws IOException
	 */
	public File tryDownloading(String url, int refreshInterval, String customTruststore)
			throws EngineException, IOException
	{		
		try
		{	//check if wait occur or file is fresh
			if (tryStartDownloading(url, refreshInterval))
			{
				return getFromCache(url, "");
			}
		} catch (InterruptedException e)
		{
			throw new InternalException("Error waiting for downloading file from " + url, e);
		}

		try
		{
			download(url, customTruststore);
		} catch (Exception ex)
		{
			log.debug("Downloading file from " + url + " fail", ex);
			endDownloading(url, false);
			log.debug("Trying get file from cache");
			return getFromCache(url, "");
		}	

		return endDownloading(url, true);
	}
	
	/**
	 * Download remote file
	 * @param url
	 * @param customTruststore
	 * @throws EngineException
	 * @throws IOException
	 */
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
	
	/**
	 * First check if cached file is fresh, if yes return true.
	 * Next check if requested file is in downloading queue, if yes waits to finishing download by another process and return true. 
	 * If file is not in download queue added it to the queue and return false.
	 * @param url
	 * @param refreshInterval
	 * @return true if waiting occurs or file is fresh, false in the other case
	 * @throws InterruptedException
	 */
	private boolean tryStartDownloading(String url, int refreshInterval) throws InterruptedException
	{
		boolean wait = false;
		synchronized (downlodingFiles)
		{	
			Long lastDownload = downloadedFiles.get(url);				
			if (lastDownload != null)
			{
				long expirationTime = System.currentTimeMillis() - (refreshInterval * 100l);
				if (lastDownload < expirationTime)
				{
					log.trace("Locally cached metadata file is fresh, skipping downloading "
							+ url);
					return true;
				}
			}
			
			while (downlodingFiles.contains(url))
			{
				if (!wait)
					log.trace("Another process is downloading metadata from " + url
							+ ", waiting to complete download");
				wait = true;
				downlodingFiles.wait();
			}
			if (!wait)
			{
				downlodingFiles.add(url);
			} else
			{
				log.trace("Stop waiting to file from " + url + ", another process end downloading");
			}
		}
		return wait;
	}

	/**
	 * Remove file from download queue, notify all waiting processes. If downloading end with success add file to downloaded map.
	 * @param url
	 * @param success
	 * @return
	 * @throws IOException 
	 */
	private File endDownloading(String url, boolean success) throws IOException
	{
		File cachedFile = null;
		synchronized (downlodingFiles)
		{
			if (success)
			{
				cachedFile = getFromCache(url, "");
				downloadedFiles.put(url, cachedFile.lastModified());

			}
			downlodingFiles.remove(url);
			downlodingFiles.notifyAll();

		}
		
		return cachedFile;
	}

	/**
	 * Get file directly from cache
	 * @param uri
	 * @param suffix
	 * @return
	 * @throws IOException
	 */
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
