/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.files;

import java.net.URI;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * 
 * @author P.Piernik
 *
 */
public interface FileStorageService
{
	public static final String UNITY_FILE_URI_SCHEMA = "unity.internal";

	public static enum StandardOwner
	{
		Authenticator
	}

	URI storageFile(byte[] content, String ownerType, String ownerId) throws EngineException;

	FileData readURI(URI uri) throws EngineException;

	FileData readImageURI(URI uri, String themeName) throws EngineException;
}
