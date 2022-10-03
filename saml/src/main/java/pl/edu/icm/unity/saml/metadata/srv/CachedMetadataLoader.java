/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;

/**
 * Downloads on demand a remote metadata file and caches it on disk. 
 * Allows for returning recently loaded file. 
 * 
 * @author K. Benedyczak
 */
public class CachedMetadataLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, CachedMetadataLoader.class);
	private final Duration PARSED_META_CACHE_TTL = Duration.ofHours(1);
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
				.expireAfterAccess(PARSED_META_CACHE_TTL).build();
	}
	
	CachedMetadataLoader(URIAccessService uriAccessService, FileStorageService fileStorageService, MetadataDownloader metadataDownloader)
	{
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
		this.downloader = metadataDownloader;
		this.parsedMetaCache = CacheBuilder.newBuilder()
				.expireAfterAccess(PARSED_META_CACHE_TTL).build();
	}
	
	/**
	 * If url is local file, then return metadata read from it, in other case try to
	 * download a remote file, cache it and read what is cached.
	 */
	public EntitiesDescriptorDocument getFresh(String rawUri, String customTruststore)
			throws EngineException, IOException, XmlException, InterruptedException
	{
		URI uri = URIHelper.parseURI(rawUri);

		EntitiesDescriptorDocument doc;
		if (!URIHelper.isWebReady(uri))
		{
			doc = EntitiesDescriptorDocument.Factory
					.parse(new ByteArrayInputStream(uriAccessService.readURI(uri).getContents()));
		} else
		{
			doc = loadFile(downloadAndCache(uri, customTruststore));
		}
		addMetaToMemoryCache(rawUri, doc);
		return doc;

	}
	

	/**
	 * @return null if there is no locally cached file or its handle
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
		EntitiesDescriptorDocument doc = loadFile(data);
		addMetaToMemoryCache(uri, doc);
		return Optional.of(doc);
	}
	
	private void addMetaToMemoryCache(String uri, EntitiesDescriptorDocument doc)
	{
		log.trace("Add metadata file for " + uri + " to memory cache");
		parsedMetaCache.put(uri, doc);
	}
	
	private EntitiesDescriptorDocument loadFile(FileData file) throws XmlException, IOException, InterruptedException
	{
		try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(file.getContents())))
		{
			String metadata = IOUtils.toString(is, Charset.defaultCharset());
			log.trace("Read metadata:\n{}", metadata);
			EntitiesDescriptorDocument doc = EntitiesDescriptorDocument.Factory.parse(metadata);
			return doc;
		}
	}
	
	private FileData downloadAndCache(URI uri, String customTruststore) throws IOException, EngineException
	{
		FileData data = downloader.download(uri, customTruststore);
		FileData savedFile = fileStorageService.storeFileInWorkspace(data.getContents(), getFileName(uri.toString()));
		log.info("Store metadata from " + uri.toString() + " in " + savedFile.getName());
		return savedFile;
	}
		
	private String getFileName(String uri)
	{
		return Paths.get(CACHE_DIR, DigestUtils.md5Hex(uri)).toString();
	}
}
