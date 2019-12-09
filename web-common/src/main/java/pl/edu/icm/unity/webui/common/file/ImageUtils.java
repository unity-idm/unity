/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import java.net.URI;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.Resource;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

public class ImageUtils
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ImageUtils.class);

	public static LocalOrRemoteResource getImageFromUriOrNull(String logoUri, URIAccessService uriService)
	{
		if (logoUri == null || logoUri.isEmpty())
			return null;
		try
		{
			URI uri = URIHelper.parseURI(logoUri);
			if (URIHelper.isWebReady(uri))
			{
				return new LocalOrRemoteResource(uri.toString());
			} else
			{
				FileData fileData = uriService.readImageURI(uri, UI.getCurrent().getTheme());
				return new LocalOrRemoteResource(fileData.getContents(), uri.toString());
			}
		} catch (Exception e)
		{
			log.error("Can not read image from uri: " + logoUri);
		}
		return null;
	}

	public static Optional<Resource> getConfiguredImageResourceFromNullableUri(String logoUri, URIAccessService uriAccessService)
	{
		if (logoUri == null || logoUri.isEmpty())
			return Optional.empty();

		try
		{
			FileData imageFileData = uriAccessService.readImageURI(URIHelper.parseURI(logoUri), 
					UI.getCurrent().getTheme());
			Resource imageResource = new FileStreamResource(imageFileData.getContents()).getResource();
			return Optional.ofNullable(imageResource);
		} catch (Exception e)
		{
			log.warn("Can not read image from uri: " + logoUri);
			return Optional.empty();
		}
	}
}
