/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms;

import io.imunity.vaadin.endpoint.common.file.DownloadHandlers;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;

import java.net.URI;
import java.util.Optional;

import com.vaadin.flow.server.streams.DownloadHandler;

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
		{
			return Optional.empty();
		}

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

		String mimeType = getMimeTypeFromFilename(fileData.getName());
		DownloadHandler downloadHandler = DownloadHandlers.forBytes(fileData.getContents(), fileData.getName(), mimeType);
		return Optional.of(new LocalOrRemoteResource(downloadHandler, "", fileData.getContents()));
	}

	private static String getMimeTypeFromFilename(String filename)
	{
		if (filename == null)
		{
			return "application/octet-stream";
		}
		String lower = filename.toLowerCase();
		if (lower.endsWith(".png"))
		{
			return "image/png";
		}
		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
		{
			return "image/jpeg";
		}
		if (lower.endsWith(".gif"))
		{
			return "image/gif";
		}
		if (lower.endsWith(".svg"))
		{
			return "image/svg+xml";
		}
		if (lower.endsWith(".webp"))
		{
			return "image/webp";
		}
		if (lower.endsWith(".bmp"))
		{
			return "image/bmp";
		}
		if (lower.endsWith(".ico"))
		{
			return "image/x-icon";
		}
		return "application/octet-stream";
	}
}
