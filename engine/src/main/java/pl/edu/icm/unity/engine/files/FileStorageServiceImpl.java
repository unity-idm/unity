/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.files;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.base.tx.Transactional;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class FileStorageServiceImpl implements FileStorageService
{
	private FileDAO fileDao;
	private String workspaceDir;
	private int fileSizeLimit;

	@Autowired
	public FileStorageServiceImpl(UnityServerConfiguration conf, FileDAO fileDao, PKIManagement pkiMan)
	{	
		workspaceDir = conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY);
		fileSizeLimit = conf.getIntValue(UnityServerConfiguration.FILE_SIZE_LIMIT);
		this.fileDao = fileDao;
	}

	@Transactional
	@Override
	public URI storeFile(byte[] content, String ownerType, String ownerId) throws EngineException
	{
		String id = UUID.randomUUID().toString();
		

		byte[] contentToSave = content == null ? new byte[0] : content;
		if (contentToSave.length > fileSizeLimit)
		{
			throw new EngineException("File content is too big, max=" + fileSizeLimit);
		}
			
		FileData fileData = new FileData(id, content, new Date(), ownerType, ownerId);
		fileDao.create(fileData);

		return URI.create(URIAccessService.UNITY_FILE_URI_SCHEMA + ":" + id);
	}

	@Override
	public FileData storeFileInWorkspace(byte[] content, String workspacePath) throws EngineException
	{
		Path toSave = Paths.get(workspaceDir, workspacePath);

		if (!toSave.normalize().startsWith(getWorkspaceRoot()))
		{
			throw new EngineException("Access denied, file " + workspacePath + " is beyond workspace dir");
		}

		byte[] contentToSave = content == null ? new byte[0] : content;
			
		try
		{
			Files.createDirectories(toSave.getParent());
			Files.write(toSave, contentToSave);

		} catch (IOException e)
		{
			throw new EngineException("Can not write to file " + toSave.toString(), e);
		}

		return new FileData(toSave.toString(), contentToSave, new Date(toSave.toFile().lastModified()));
	}

	@Override
	public FileData readFileFromWorkspace(String workspacePath) throws EngineException
	{
		Path toRead = Paths.get(workspaceDir, workspacePath);
	
		if (!toRead.normalize().startsWith(getWorkspaceRoot()))
		{
			throw new EngineException("Access denied, file " + workspacePath + " is beyond workspace dir");
		}
		
		try
		{
			return new FileData(toRead.toString(), Files.readAllBytes(toRead),
					new Date(toRead.toFile().lastModified()));
		} catch (IOException e)
		{
			throw new EngineException("Can not read file " + toRead.toString(), e);
		}
	}
	
	private Path getWorkspaceRoot() throws EngineException
	{
		return Paths.get(workspaceDir).normalize();
	}
}
