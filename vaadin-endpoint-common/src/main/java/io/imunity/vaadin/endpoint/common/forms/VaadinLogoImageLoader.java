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
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Optional;

@Component
public class VaadinLogoImageLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, VaadinLogoImageLoader.class);
	private static final String LOCAL_GW_PREFIX = "../unitygw";
	private final URIAccessService uriAccessService;

	VaadinLogoImageLoader(URIAccessService uriAccessService)
	{
		this.uriAccessService = uriAccessService;
	}

	public Optional<LocalOrRemoteResource> loadImageFromUri(String logoUri)
	{
		if (logoUri == null || logoUri.isEmpty())
			return Optional.empty();
		if (logoUri.startsWith(LOCAL_GW_PREFIX))
		{
			return Optional.of(new LocalOrRemoteResource(logoUri, ""));
		}
			
		URI uri;
		try
		{
			uri = URIHelper.parseURI(logoUri);
		} catch (IllegalURIException e1)
		{
			log.error("Can not parse image uri  " + logoUri);
			return Optional.empty();
		}

		if (URIHelper.isWebReady(uri))
		{
			return Optional.of(new LocalOrRemoteResource(uri.toString(), ""));
		} else
		{
			FileData fileData = null;
			try
			{
				fileData = uriAccessService.readImageURI(uri, ImageAccessService.UNKNOWN_THEME);
			} catch (Exception e)
			{
				log.error("Can not read image from uri: " + logoUri);
			}
			
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileData.getContents());
			StreamResource streamResource = new StreamResource(fileData.getName(), () -> byteArrayInputStream);
			return Optional.of(new LocalOrRemoteResource(streamResource, "", fileData.getContents()));
		}

	}
}
