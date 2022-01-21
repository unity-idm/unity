/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.files;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;

@Component
public class URIAccessServiceImpl implements URIAccessService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, URIAccessServiceImpl.class);
	
	private FileDAO fileDao;
	private boolean restrictFileSystemAccess;
	private String webContentDir;
	private RemoteFileNetworkClient fileNetworkClient;

	@Autowired
	public URIAccessServiceImpl(UnityServerConfiguration conf, FileDAO fileDao, PKIManagement pkiMan)
	{
		restrictFileSystemAccess = conf.getBooleanValue(UnityServerConfiguration.RESTRICT_FILE_SYSTEM_ACCESS);
		webContentDir = conf.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		this.fileDao = fileDao;
		fileNetworkClient = new RemoteFileNetworkClient(pkiMan);
	}
	
	public URIAccessServiceImpl(UnityServerConfiguration conf, FileDAO fileDao, RemoteFileNetworkClient fileNetworkClient)
	{
		restrictFileSystemAccess = conf.getBooleanValue(UnityServerConfiguration.RESTRICT_FILE_SYSTEM_ACCESS);
		webContentDir = conf.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		this.fileDao = fileDao;
		this.fileNetworkClient = fileNetworkClient;
	}
		
	@Transactional
	@Override
	public FileData readURI(URI uri)
	{	
		try
		{	URIHelper.validateURI(uri);
			return readUriInternal("", uri, null);
		} catch (EngineException e)
		{
			log.error("Can not read uri: " + uri.toString(), e);
			throw new URIAccessException("Can not read uri", e);
		}
	}
	
	@Transactional
	@Override
	public FileData readURI(URI uri, String customTruststore)
	{
		try
		{
			URIHelper.validateURI(uri);
			return readUriInternal("", uri, customTruststore);
		} catch (EngineException e)
		{
			log.trace("Can not read uri: " + uri.toString(), e);
			throw new URIAccessException("Can not read uri", e);
		}
	}

	@Transactional
	@Override
	public FileData readImageURI(URI uri, String themeName)
	{
		if (themeName == null)
		{
			throw new IllegalArgumentException("Theme name can not be null");
		}
		
		try
		{
			URIHelper.validateURI(uri);
		} catch (IllegalURIException e)
		{
			throw new URIAccessException("Can not read image uri: " + uri.toString(), e);
		}
		String root = Paths.get(webContentDir, "VAADIN", "themes", themeName).toFile().getAbsolutePath();

		try
		{
			return readUriInternal(root, uri, null);
		} catch (EngineException e)
		{
			log.trace("Can not read image file from uri", e);
		}

		try
		{
			return readImageFileFromClassPath(themeName, URIHelper.getPathFromURI(uri));
		} catch (IOException e)
		{
			log.trace("Can not read image file from classpath", e);
		}

		log.warn("Can not read image uri: " + uri.toString());
		throw new URIAccessException("Can not read image uri: " + uri.toString());
	}

	private FileData readUriInternal(String root, URI uri, String customTrustStore) throws EngineException
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
				return readURL(uri.toURL(), customTrustStore);
			} catch (Exception e)
			{
				throw new EngineException("Can not read URL, uri: " + uri.toString(), e);
			}

		} else if (uri.getScheme().equals("data"))
		{

			try
			{
				return readDataScheme(URIHelper.getPathFromURI(uri));
			} catch (Exception e)
			{
				throw new EngineException("Can not read data uri: " + uri.toString(), e);
			}
		}

		else if (uri.getScheme().equals(UNITY_FILE_URI_SCHEMA))
		{

			try
			{
				return fileDao.get(URIHelper.getPathFromURI(uri));
			} catch (IllegalArgumentException e)
			{
				throw new EngineException("Can not read internal unity file, uri: " + uri.toString(),
						e);
			}
		} else
		{
			throw new IllegalURIException("Not supported uri schema");
		}
	}

	

	private FileData readDataScheme(String data) throws IllegalURIException
	{
		if (data == null)
			throw new IllegalURIException("Data element of uri can not be empty");

		String pureBase64 = data.contains(",") ? data.substring(data.indexOf(",") + 1) : data;
		byte[] decoded = Base64.getDecoder().decode(pureBase64);
		return new FileData("", decoded, new Date());
	}

	private FileData readURL(URL url, String customTruststore) throws IOException, EngineException
	{
		return new FileData(url.toString(), fileNetworkClient.download(url, customTruststore), new Date());
	}

	private FileData readRestrictedFile(URI uri, String root) throws IOException, IllegalURIException
	{
		Path toRead = getRealFilePath(root, URIHelper.getPathFromURI(uri));

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

		File read = toRead.toFile();
		log.debug("Read file from path: " + toRead.toString());
		
		return new FileData(read.getName(), Files.readAllBytes(toRead), new Date(read.lastModified()));
	}

	private FileData readUnRestrictedFile(URI uri, String root) throws IOException
	{
		Path toRead = getRealFilePath(root, URIHelper.getPathFromURI(uri));
		File read = toRead.toFile();
		log.debug("Read file from path: " + toRead.toString());
		
		return new FileData(read.getName(), Files.readAllBytes(toRead), new Date(read.lastModified()));
	}

	private Path getRealFilePath(String rootPath, String filePath) throws IOException
	{
		String root = new File(rootPath).getAbsolutePath();
		try
		{
			return Paths.get(root, filePath).toRealPath();
		} catch (Exception e)
		{
			log.warn("Can not read file: " + e.toString());
			throw new IOException("File does not exists");
		}
	}

	private FileData readImageFileFromClassPath(String themeName, String path) throws IOException
	{
		Resource r = new ClassPathResource(Paths.get("VAADIN", "themes", themeName, path).toString());
		return new FileData(new File(path).getName(), IOUtils.toByteArray(r.getInputStream()), new Date());
	}
}
