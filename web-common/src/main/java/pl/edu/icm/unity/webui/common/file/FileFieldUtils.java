/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import java.net.URI;
import java.util.Properties;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

/**
 * File field related methods.
 * 
 * @author P.Piernik
 *
 */
public class FileFieldUtils
{
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
		if (res != null)
		{	
			if (res.getRemote() != null && !res.getRemote().isEmpty())
			{
				return res.getRemote();
			} else if (res.getLocalUri() != null)
			{
				return res.getLocalUri();
			}else if (res.getLocal() != null)
			{
				try
				{
					URI uri = fileStorageService.storeFile(res.getLocal(), ownerType,
							ownerId);
					return uri.toString();
				} catch (EngineException e)
				{
					throw new InternalException("Can't save file into DB", e);
				}
			}
		}

		return null;
	}
}
