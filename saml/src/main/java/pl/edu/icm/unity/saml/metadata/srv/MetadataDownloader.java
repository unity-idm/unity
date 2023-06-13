/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.io.IOException;
import java.net.URI;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.URIAccessService;

class MetadataDownloader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, MetadataDownloader.class);
	private final URIAccessService uriAccessService;
	
	MetadataDownloader(URIAccessService uriAccessService)
	{
		this.uriAccessService = uriAccessService;

	}

	FileData download(URI uri, String customTruststore) throws IOException, EngineException
	{
		 FileData file = uriAccessService.readURI(uri, customTruststore);
		 log.info("Downloaded metadata from " + uri.toString());
		 return file;
	}
}
