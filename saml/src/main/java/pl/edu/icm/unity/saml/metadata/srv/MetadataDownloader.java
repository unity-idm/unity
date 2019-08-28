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
	private final URIAccessService uriAccessService;
	private final FileStorageService fileStorageService;

	public MetadataDownloader(URIAccessService uriAccessService, FileStorageService fileStorageService)
	{
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
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
			return loadFile(download(uri, customTruststore));
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
		FileData data;
		try
		{
			data = fileStorageService.readFileFromWorkspace(getFileName(uri));
		} catch (Exception e)
		{
			return Optional.empty();
		}
		log.debug("Get metadata file for "+ uri + " from cache");
		return Optional.of(loadFile(new ByteArrayInputStream(data.getContents())));
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
		FileData data = uriAccessService.readURI(uri, customTruststore);
		FileData savedFile = fileStorageService.storeFileInWorkspace(data.getContents(), getFileName(uri.toString()));
		log.info("Downloaded metadata from " + uri.toString() + " and stored in " + savedFile.getName());
		return new ByteArrayInputStream(savedFile.getContents());
	}
		
	public static String getFileName(String uri)
	{
		return Paths.get(CACHE_DIR, DigestUtils.md5Hex(uri)).toString();
	}
}
