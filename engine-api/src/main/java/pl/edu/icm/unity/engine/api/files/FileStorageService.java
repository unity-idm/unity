/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.files;

import java.net.URI;
import java.util.Optional;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Provides access to local or remote file. 
 * 
 * @author P.Piernik
 *
 */
public interface FileStorageService
{
	public static final String UNITY_FILE_URI_SCHEMA = "unity.internal";

	public static enum StandardOwner
	{
		Authenticator, Form
	}

	URI storageFile(byte[] content, String ownerType, String ownerId) throws EngineException;

	FileData readURI(URI uri, Optional<String> customTruststore) throws EngineException;

	FileData readImageURI(URI uri, String themeName) throws EngineException;

	FileData stoarageFileInWorkspace(byte[] content, String workspacePath) throws EngineException;

	FileData readFileFromWorkspace(String workspacePath) throws EngineException;
}
