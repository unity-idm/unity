/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import java.net.URI;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;

import com.vaadin.server.Resource;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

/**
 * 
 * @author P.Piernik
 *
 */
public class ImageUtils
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, FileFieldUtils.class);

	public static LocalOrRemoteResource getImageFromUriSave(String logoUri, URIAccessService uriService)
	{
		if (logoUri == null || logoUri.isEmpty())
		{
			return null;
		}
		
		
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

	public static Resource getConfiguredImageResourceFromUri(String logoUri, URIAccessService uriAccessService)
	{

		if (logoUri == null || logoUri.isEmpty())
		{
			return null;
		}

		try
		{
			return new FileStreamResource(uriAccessService.readImageURI(URIHelper.parseURI(logoUri),
					UI.getCurrent().getTheme()).getContents()).getResource();
		} catch (Exception e)
		{
			log.warn("Can not read image from uri: " + logoUri);
			throw new ConfigurationException("Can not load configured image " + logoUri, e);
		}
	}
	
	public static Resource getConfiguredImageResourceFromUriSave(String logoUri, URIAccessService uriAccessService)
	{
		try
		{
			return getConfiguredImageResourceFromUri(logoUri, uriAccessService);

		} catch (Exception e)
		{
			return null;
		}
	}

}
