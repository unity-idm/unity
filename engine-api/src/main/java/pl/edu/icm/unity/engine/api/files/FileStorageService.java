/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.files;

import java.net.URI;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.file.FileData;

/**
 * Provides access to local or remote file. 
 * 
 * @author P.Piernik
 *
 */
public interface FileStorageService
{
	public static enum StandardOwner
	{
		AUTHENTICATOR, FORM, SERVICE,
	}

	URI storeFile(byte[] content, String ownerType, String ownerId) throws EngineException;

	FileData storeFileInWorkspace(byte[] content, String workspacePath) throws EngineException;

	FileData readFileFromWorkspace(String workspacePath) throws EngineException;
}
