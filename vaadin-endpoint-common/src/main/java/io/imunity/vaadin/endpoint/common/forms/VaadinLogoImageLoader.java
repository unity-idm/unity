/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms;

import com.vaadin.flow.server.StreamResource;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.engine.api.files.URIAccessService;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Optional;

@Component
public class VaadinLogoImageLoader
{
	private final URIAccessService uriAccessService;

	VaadinLogoImageLoader(URIAccessService uriAccessService)
	{
		this.uriAccessService = uriAccessService;
	}

	public Optional<LocalOrRemoteResource> loadImageFromUri(String url)
	{
		if(url == null || url.isEmpty())
			return Optional.empty();
		if(url.startsWith(uriAccessService.UNITY_FILE_URI_SCHEMA))
		{
			FileData fileData = uriAccessService.readURI(URI.create(url));
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileData.getContents());
			StreamResource streamResource = new StreamResource(fileData.getName(), () -> byteArrayInputStream);
			return Optional.of(new LocalOrRemoteResource(streamResource, "", fileData.getContents()));
		}
		else
			return Optional.of(new LocalOrRemoteResource(url, ""));
	}
}
