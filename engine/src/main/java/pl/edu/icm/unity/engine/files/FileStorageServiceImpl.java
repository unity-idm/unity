/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalURIException;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class FileStorageServiceImpl implements FileStorageService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, FileStorageServiceImpl.class);

	private FileDAO fileDao;
	private boolean restrictFileSystemAccess;
	private String webContentDir;

	@Autowired
	public FileStorageServiceImpl(UnityServerConfiguration conf, FileDAO fileDao)
	{
		restrictFileSystemAccess = conf.getBooleanValue(UnityServerConfiguration.RESTRICT_FILE_SYSTEM_ACCESS);
		webContentDir = conf.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		this.fileDao = fileDao;
	}

	@Transactional
	@Override
	public URI storageFile(byte[] content, String ownerType, String ownerId) throws EngineException
	{
		String id = UUID.randomUUID().toString();
		FileData fileData = new FileData(id, content);
		fileData.setOwnerType(ownerType);
		fileData.setOwnerId(ownerId);
		fileDao.create(fileData);

		return URI.create(UNITY_FILE_URI_SCHEMA + ":" + id);
	}

	@Transactional
	@Override
	public FileData readURI(URI uri) throws EngineException
	{
		URIHelper.validateURI(uri);
		try
		{
			return readUriInternal("", uri);
		} catch (EngineException e)
		{
			log.debug("Can not read file from uri: " + uri.toString(), e);
			throw e;
		}
	}

	@Transactional
	@Override
	public FileData readImageURI(URI uri, String themeName) throws EngineException
	{
		URIHelper.validateURI(uri);
		String root = Paths.get(webContentDir, "VAADIN", "themes", themeName).toFile().getAbsolutePath();

		try
		{
			return readUriInternal(root, uri);
		} catch (EngineException e)
		{
			log.trace("Can not read image file from disk", e);
		}

		try
		{
			return readImageFileFromClassPath(themeName, getPathFromURI(uri));
		} catch (IOException e)
		{
			log.trace("Can not read image file from classpath", e);
		}

		log.debug("Can not read image file from uri " + uri.toString());
		throw new EngineException("Can not read image file from uri" + uri.toString());
	}

	@Transactional
	private FileData readUriInternal(String root, URI uri) throws EngineException
	{

		if (uri.getScheme() == null || uri.getScheme().isEmpty() || uri.getScheme().equals("file"))
		{
			try
			{
				if (!restrictFileSystemAccess)
				{
					return readUnRestrictedFile(uri, root);
				} else
				{
					return readRestrictedFile(uri, root);
				}
			} catch (Exception e)
			{

				throw new EngineException("Can not read file from uri: " + uri.toString(), e);
			}
		} else if (uri.getScheme().equals("http") || uri.getScheme().equals("https"))
		{
			try
			{
				return readURL(uri.toURL());
			} catch (Exception e)
			{
				throw new EngineException("Can not read URL uri: " + uri.toString(), e);
			}

		} else if (uri.getScheme().equals("data"))
		{

			try
			{
				return readDataScheme(getPathFromURI(uri));
			} catch (Exception e)
			{
				throw new EngineException("Can not read data uri: " + uri.toString(), e);
			}
		}

		else if (uri.getScheme().equals(UNITY_FILE_URI_SCHEMA))
		{

			try
			{
				return fileDao.get(getPathFromURI(uri));
			} catch (IllegalArgumentException e)
			{
				throw new EngineException("Can not read internal unity uri: " + uri.toString(), e);
			}
		} else
		{
			throw new IllegalURIException("Not supported uri schema");
		}
	}

	private String getPathFromURI(URI uri)
	{
		return uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath();
	}
	
	private FileData readDataScheme(String data) throws IllegalURIException
	{
		if (data == null)
			throw new IllegalURIException("Data element of uri can not be empty");

		byte[] decoded = Base64.getDecoder().decode(data);
		return new FileData("", decoded);
	}

	private FileData readURL(URL url) throws IOException
	{
		BufferedInputStream in;
		in = new BufferedInputStream(url.openStream());
		byte[] bytes = IOUtils.toByteArray(in);
		return new FileData("", bytes);
	}

	private FileData readRestrictedFile(URI uri, String root) throws IOException, IllegalURIException
	{
		Path toRead = getRealFilePath(root, getPathFromURI(uri));

		Path realRoot;
		try
		{
			realRoot = Paths.get(new File(webContentDir).getAbsolutePath()).toRealPath();
		} catch (IOException e)
		{
			throw new IOException("Web content dir does not exists");
		}

		if (!toRead.startsWith(realRoot))
		{
			throw new IOException("Access to file is limited");
		}

		return new FileData(toRead.toFile().getName(), Files.readAllBytes(toRead));
	}

	private FileData readUnRestrictedFile(URI uri, String root) throws IOException
	{
		Path toRead = getRealFilePath(root, getPathFromURI(uri));
		return new FileData(toRead.toFile().getName(), Files.readAllBytes(toRead));
	}

	private Path getRealFilePath(String rootPath, String filePath) throws IOException
	{
		String root = new File(rootPath).getAbsolutePath();
		try
		{
			return Paths.get(root, filePath).toRealPath();
		} catch (Exception e)
		{
			throw new IOException("File does not exists");
		}
	}

	private FileData readImageFileFromClassPath(String themeName, String path) throws IOException
	{
		Resource r = new ClassPathResource(Paths.get("VAADIN", "themes", themeName, path).toString());
		return new FileData(new File(path).getName(), Files.readAllBytes(Paths.get(r.getFile().getPath())));
	}
}
