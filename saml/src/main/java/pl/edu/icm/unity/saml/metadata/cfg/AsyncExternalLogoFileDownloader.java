/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Base64.getDecoder;

@Component
class AsyncExternalLogoFileDownloader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, AsyncExternalLogoFileDownloader.class);
	private static final String STAGING = "staging";
	private final ExecutorService executorService;
	private final String workspaceDir;
	private final String defaultLocale;
	private final URIAccessService uriAccessService;

	public AsyncExternalLogoFileDownloader(UnityServerConfiguration conf, MessageSource msg, URIAccessService uriAccessService)
	{
		workspaceDir = conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY);
		executorService = Executors.newFixedThreadPool(Integer.parseInt(conf.getValue(UnityServerConfiguration.THREAD_POOL_SIZE)));
		defaultLocale = msg.getLocale().toString();
		this.uriAccessService = uriAccessService;
	}

	public void downloadLogoFilesAsync(TrustedIdPs trustedIdPs, String httpsTruststore)
	{
		CompletableFuture.runAsync(() ->
				trustedIdPs.getEntrySet().forEach(entry -> downloadFiles(entry, httpsTruststore)),
				executorService
		).thenRunAsync(() ->
						trustedIdPs.getEntrySet().stream()
								.map(entry -> federationDirName(entry.getValue().federationId))
								.distinct()
								.forEach(this::copyToAndCleanFinalDir),
						executorService
		);
	}

	private void copyToAndCleanFinalDir(String catalog)
	{
		try
		{
			Set<Path> stringNames;
			Path stagingDir = Paths.get(workspaceDir, STAGING, catalog);
			try (Stream<Path> paths = Files.walk(stagingDir))
			{
				stringNames = paths.filter(Files::isRegularFile)
						.map(Path::getFileName)
						.collect(Collectors.toSet());
			}

			Set<Path> finalNames;
			Path finalDir = Paths.get(workspaceDir, catalog);
			finalDir.toFile().mkdir();
			try (Stream<Path> paths = Files.walk(finalDir))
			{
				finalNames = paths.filter(Files::isRegularFile)
						.map(Path::getFileName)
						.collect(Collectors.toSet());
			}

			finalNames.removeAll(stringNames);
			try (Stream<Path> paths = Files.walk(finalDir))
			{
				paths.filter(path -> finalNames.contains(path.getFileName()))
						.forEach(path -> path.toFile().delete());
			}
			FileUtils.copyDirectory(stagingDir.toFile(), finalDir.toFile());
			FileUtils.deleteDirectory(stagingDir.toFile());
		}
		catch (IOException e)
		{
			log.info(e);
		}
	}

	private void downloadFiles(Map.Entry<TrustedIdPKey, TrustedIdPConfiguration> entry, String httpsTruststore)
	{
		entry.getValue().logoURI
				.getMap()
				.forEach((locale, uri) ->
						{
							String federationDirName = federationDirName(entry.getValue().federationId);
							String logoFileBasename = getLogoFileBasename(entry.getKey(), new Locale(locale), defaultLocale);
							saveFileOnDisk(federationDirName, logoFileBasename, uri, httpsTruststore);
						}
				);
	}

	private void saveFileOnDisk(String catalog, String name, String logoURI, String httpsTruststore)
	{
		try
		{
			URI uri = URI.create(logoURI);
			if(uri.getScheme().equals("data"))
				saveFileBasedOnDataURI(catalog, name, logoURI);
			else
				downloadFile(catalog, name, uri, httpsTruststore);
		} catch (Exception e)
		{
			log.debug("Logo file with uri {} cannot be downloaded: {}", logoURI, e.getMessage());
		}
	}

	private void downloadFile(String catalog, String name, URI uri, String httpsTruststore) throws IOException
	{
		FileData fileData = uriAccessService.readURI(uri, httpsTruststore);
		File yourFile = createFile(catalog, name);
		Files.write(yourFile.toPath(), fileData.getContents());
	}

	private void saveFileBasedOnDataURI(String catalog, String name, String logoURI) throws IOException
	{
		int dataStartIndex = logoURI.indexOf(",") + 1;
		String data = logoURI.substring(dataStartIndex);
		byte[] decoded = getDecoder().decode(data);
		File yourFile = createFile(catalog, name);
		Files.write(yourFile.toPath(), decoded);
	}

	private File createFile(String catalog, String name) throws IOException
	{
		new File(Path.of(workspaceDir, STAGING, catalog).toUri()).mkdirs();
		File yourFile = new File(Path.of(workspaceDir, STAGING, catalog, name).toUri());
		yourFile.createNewFile();
		return yourFile;
	}

	static String getLogoFileBasename(TrustedIdPKey trustedIdPKey, Locale locale, String defaultLocale)
	{
		return trustedIdPKey.getSourceData()
				.map(metadata -> metadata.entityHex + metadata.index)
				.orElse(trustedIdPKey.asString()) + (locale.toString().isBlank() ? defaultLocale : locale);
	}

	static String getLogoFileBasename(TrustedIdPKey trustedIdPKey, String defaultLocale)
	{
		return trustedIdPKey.getSourceData()
				.map(metadata -> metadata.entityHex + metadata.index)
				.orElse(trustedIdPKey.asString()) + defaultLocale;
	}

	static String federationDirName(String federationId)
	{
		return DigestUtils.md5Hex(federationId);
	}
}
