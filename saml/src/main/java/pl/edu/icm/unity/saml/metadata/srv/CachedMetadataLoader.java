/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

/**
 * Downloads on demand a remote metadata file and caches it on disk. 
 * Allows for returning recently loaded file. 
 * 
 * @author K. Benedyczak
 */
public class CachedMetadataLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, CachedMetadataLoader.class);
	private final int PARSED_META_CACHE_TTL_IN_HOURS = 1;
	private static final String CACHE_DIR = "downloadedMetadata";
	private final URIAccessService uriAccessService;
	private final FileStorageService fileStorageService;
	private final MetadataDownloader downloader;
	private final Cache<String, EntitiesDescriptorDocument> parsedMetaCache;
	
	
	public CachedMetadataLoader(URIAccessService uriAccessService, FileStorageService fileStorageService)
	{
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
		this.downloader = new MetadataDownloader(uriAccessService);
		this.parsedMetaCache = CacheBuilder.newBuilder()
				.expireAfterAccess(PARSED_META_CACHE_TTL_IN_HOURS, TimeUnit.HOURS).build();
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
	public EntitiesDescriptorDocument getFresh(String rawUri, String customTruststore)
			throws EngineException, IOException, XmlException, InterruptedException
	{
		URI uri = URIHelper.parseURI(rawUri);
		
		if (!URIHelper.isWebReady(uri))
		{
			return EntitiesDescriptorDocument.Factory.parse(new ByteArrayInputStream(uriAccessService.readURI(uri).getContents()));
		} else
		{
			EntitiesDescriptorDocument doc = loadFile(download(uri, customTruststore));
			addMetaToMemoryCache(rawUri, doc);
			return doc;
		}
	}

	/**
	 * @return null if there is no locally cached file or its handle
	 * @throws IOException 
	 * @throws XmlException 
	 * @throws InterruptedException 
	 */
	public Optional<EntitiesDescriptorDocument> getCached(String uri)
			throws XmlException, IOException, InterruptedException
	{
		EntitiesDescriptorDocument cached = parsedMetaCache.getIfPresent(uri);
		if (cached != null)
		{
			log.debug("Get metadata file for "+ uri + " from memory cache");
			return Optional.of(cached);
		}
		
		FileData data;
		try
		{
			data = fileStorageService.readFileFromWorkspace(getFileName(uri));
		} catch (Exception e)
		{
			return Optional.empty();
		}
		log.debug("Get metadata file for "+ uri + " from downloaded files cache");
		EntitiesDescriptorDocument doc = loadFile(new ByteArrayInputStream(data.getContents()));
		addMetaToMemoryCache(uri, doc);
		return Optional.of(doc);
	}
	
	private void addMetaToMemoryCache(String uri, EntitiesDescriptorDocument doc)
	{
		log.trace("Add metadata file for " + uri + " to memory cache");
		parsedMetaCache.put(uri, doc);
	}
	
	private EntitiesDescriptorDocument loadFile(InputStream file) throws XmlException, IOException, InterruptedException
	{
		InputStream is = new BufferedInputStream(file);
		String metadata = IOUtils.toString(is, Charset.defaultCharset());
		log.trace("Read metadata:\n{}", metadata);
		EntitiesDescriptorDocument doc = EntitiesDescriptorDocument.Factory.parse(metadata);
		is.close();
		return doc;
	}
	
	private InputStream download(URI uri, String customTruststore) throws IOException, EngineException
	{
		FileData data = downloader.download(uri, customTruststore);
		FileData savedFile = fileStorageService.storeFileInWorkspace(data.getContents(), getFileName(uri.toString()));
		log.info("Store metadata from " + uri.toString() + " in " + savedFile.getName());
		return new ByteArrayInputStream(savedFile.getContents());
	}
		
	public static String getFileName(String uri)
	{
		return Paths.get(CACHE_DIR, DigestUtils.md5Hex(uri)).toString();
	}
}
