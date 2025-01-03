/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms;

import com.vaadin.flow.server.StreamResource;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Optional;

@Component
public class VaadinLogoImageLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, VaadinLogoImageLoader.class);
	private final URIAccessService uriAccessService;

	VaadinLogoImageLoader(URIAccessService uriAccessService)
	{
		this.uriAccessService = uriAccessService;
	}

	public Optional<LocalOrRemoteResource> loadImageFromUri(String logoUri)
	{
		if (logoUri == null || logoUri.isEmpty())
			return Optional.empty();
			
		URI uri;
		try
		{
			uri = URIHelper.parseURI(logoUri);
		} catch (IllegalURIException e1)
		{
			log.error("Can not parse image URI  " + logoUri);
			return Optional.empty();
		}

		return URIHelper.isWebReady(uri) ?
			Optional.of(new LocalOrRemoteResource(uri.toString(), "")) :
			fetchAndExpose(logoUri, uri);

	}

	private Optional<LocalOrRemoteResource> fetchAndExpose(String logoUri, URI uri)
	{
		FileData fileData;
		try
		{
			fileData = uriAccessService.readImageURI(uri);
		} catch (Exception e)
		{
			log.error("Can not read image from URI: " + logoUri);
			return Optional.empty();
		}
		
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileData.getContents());
		StreamResource streamResource = new StreamResource(fileData.getName(), () -> byteArrayInputStream);
		return Optional.of(new LocalOrRemoteResource(streamResource, "", fileData.getContents()));
	}
}
