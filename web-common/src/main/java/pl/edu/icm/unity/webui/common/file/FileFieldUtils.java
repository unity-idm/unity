/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import java.net.URI;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.UI;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

/**
 * Logo field related methods.
 * 
 * @author P.Piernik
 *
 */
public class FileFieldUtils
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, FileFieldUtils.class);
	
	public static LocalOrRemoteResource getLogoResourceFromUri(String logoUri,
			FileStorageService fileStorageService) 
	{	
		try
		{
			URI uri = URIHelper.parseURI(logoUri);
			if (URIHelper.isWebReady(uri))
			{
				return new LocalOrRemoteResource(uri.toString());
			} else
			{
				FileData fileData = fileStorageService.readImageURI(uri, UI.getCurrent().getTheme());
				return new LocalOrRemoteResource(fileData.getContents(), uri.toString());
			}
			
			
		} catch (EngineException e)
		{
			log.error("Can not read logo from uri: " + logoUri);
		}
		
		return null;
	}

	public static void saveInProperties(LocalOrRemoteResource res, String prefix, Properties raw,
			FileStorageService fileStorageService, String ownerType, String ownerId)
	{	
		String uri = saveFile(res, fileStorageService, ownerType, ownerId);
		if (uri != null)
		{
			raw.put(prefix, uri);
		}	
	}

	public static String saveFile(LocalOrRemoteResource res, FileStorageService fileStorageService,
			String ownerType, String ownerId)
	{
		if (res.getLocal() != null)
		{
			if (res.getLocalUri() != null)
			{
				return res.getLocalUri();
			} else
			{
				try
				{
					URI uri = fileStorageService.storageFile(res.getLocal(), ownerType, ownerId);
					return uri.toString();
				} catch (EngineException e)
				{
					throw new InternalException("Can't save file into DB", e);
				}
			}
		} else if (res.getRemote() != null && !res.getRemote().isEmpty())
		{
			return res.getRemote();
		}

		return null;
	}
}
