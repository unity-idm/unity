/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Downloads on demand a remote metadata file and caches it on disk. 
 * Allows for returning recently loaded file. 
 * 
 * @author K. Benedyczak
 */
public class MetadataDownloader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, MetadataDownloader.class);
	private static final String CACHE_DIR = "downloadedMetadata";

	private final String workspaceDirectory;
	private final NetworkClient client;

	public MetadataDownloader(String workspaceDirectory, NetworkClient client)
	{
		this.workspaceDirectory = workspaceDirectory;
		this.client = client;
	}

	
	/**
	 * If url is local file, then return metadata read from it,
	 * in other case try to download a remote file, cache it and read what is cached.
	 * 
	 * @param url
	 * @param customTruststore
	 * @return
	 * @throws EngineException
	 * @throws IOException
	 * @throws XmlException
	 * @throws InterruptedException 
	 */
	EntitiesDescriptorDocument getFresh(String url, String customTruststore)
			throws EngineException, IOException, XmlException, InterruptedException
	{
		if (url.startsWith("file:"))
		{
			URL localUrl = new URL(url);
			return EntitiesDescriptorDocument.Factory.parse(localUrl.openStream());
		} else
		{
			File downloaded = download(url, customTruststore);
			return loadFile(downloaded);
		}
	}

	/**
	 * @return null if there is no locally cached file or its handle
	 * @throws IOException 
	 * @throws XmlException 
	 * @throws InterruptedException 
	 */
	Optional<EntitiesDescriptorDocument> getCached(String url) throws XmlException, IOException, InterruptedException
	{
		File cached = getLocalFile(url, "");
		if (!cached.exists())
			return Optional.empty();
		return Optional.of(loadFile(cached));
	}
	
	private EntitiesDescriptorDocument loadFile(File file) throws XmlException, IOException, InterruptedException
	{
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		String metadata = IOUtils.toString(is);
		log.trace("Read metadata:\n{}", metadata);
		EntitiesDescriptorDocument doc = EntitiesDescriptorDocument.Factory.parse(metadata);
		is.close();
		return doc;
	}
	
	private File download(String url, String customTruststore) throws IOException, EngineException
	{
		File cachedFile = getLocalFile(url, "");
		File cachedFilePart = getLocalFile(url, "_part");
		if (cachedFilePart.exists())
			cachedFilePart.delete();

		log.debug("Downloading metadata from " + url + " to " + cachedFilePart.toString());
		InputStream is = client.download(url, customTruststore);
		FileOutputStream cacheFos = new FileOutputStream(cachedFilePart);
		IOUtils.copy(is, cacheFos);
		cacheFos.close();
		cachedFile.delete();
		FileUtils.moveFile(cachedFilePart, cachedFile);
		log.info("Downloaded metadata from " + url + " and stored in "
				+ cachedFile.toString());
		return cachedFile;
	}
	
	private File getLocalFile(String uri, String suffix)
	{
		File dir = new File(workspaceDirectory, CACHE_DIR);
		if (!dir.exists())
			dir.mkdirs();
		return new File(dir, DigestUtils.md5Hex(uri) + suffix);
	}
}
