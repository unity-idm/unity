/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Base64.getDecoder;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;

@Component
public class AsyncExternalLogoFileDownloader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, AsyncExternalLogoFileDownloader.class);
	private static final String STAGING = "staging";
	private final ExecutorService executorService;
	private final URIAccessService uriAccessService;

	private final MetadataToSPConfigConverter converter;
	private final String workspaceDir;
	private final String defaultLocale;

	public AsyncExternalLogoFileDownloader(UnityServerConfiguration conf, MessageSource msg, URIAccessService uriAccessService,
	                                       ExecutorsService executorsService, MetadataToSPConfigConverter converter)
	{
		workspaceDir = conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY);
		executorService = executorsService.getService();
		defaultLocale = msg.getLocale().toString();
		this.uriAccessService = uriAccessService;
		this.converter = converter;
	}

	public void downloadLogoFilesAsync(EntitiesDescriptorDocument entitiesDescriptorDocument, String httpsTruststore)
	{
		RemoteMetadataSource build = RemoteMetadataSource.builder()
				.withTranslationProfile(new TranslationProfile("mock", "description", ProfileType.INPUT, List.of()))
				.withUrl("url")
				.withRefreshInterval(Duration.ZERO)
				.build();
		TrustedIdPs trustedIdPs = converter.convertToTrustedIdPs(entitiesDescriptorDocument, build);
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
			Set<Path> stagingDestinationFileNames;
			Path stagingDir = Paths.get(workspaceDir, STAGING, catalog);
			stagingDestinationFileNames = getFileNamesFromDir(stagingDir);

			Set<Path> finalDestinationFileNames;
			Path finalDir = Paths.get(workspaceDir, catalog);
			finalDir.toFile().mkdir();
			finalDestinationFileNames = getFileNamesFromDir(finalDir);

			removeFileFromFinalDestinationWhichDoNotHaveEquivalentInStageDestination(finalDestinationFileNames, stagingDestinationFileNames, finalDir);
			copyDirectory(stagingDir.toFile(), finalDir.toFile());
			deleteDirectory(stagingDir.toFile());
		}
		catch (IOException e)
		{
			log.info("Failed while coping/cleaning images from stage to final destination", e);
		}
	}

	private static void removeFileFromFinalDestinationWhichDoNotHaveEquivalentInStageDestination(Set<Path> finalDestinationFileNames,
	                                                                                             Set<Path> stagingDestinationFileNames,
	                                                                                             Path finalDir) throws IOException
	{
		finalDestinationFileNames.removeAll(stagingDestinationFileNames);
		try (Stream<Path> paths = Files.walk(finalDir))
		{
			paths.filter(path -> finalDestinationFileNames.contains(path.getFileName()))
					.forEach(path -> path.toFile().delete());
		}
	}

	private static Set<Path> getFileNamesFromDir(Path stagingDir) throws IOException
	{
		Set<Path> stagingDestinationFileNames;
		try (Stream<Path> paths = Files.walk(stagingDir))
		{
			stagingDestinationFileNames = paths.filter(Files::isRegularFile)
					.map(Path::getFileName)
					.collect(Collectors.toSet());
		}
		return stagingDestinationFileNames;
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
		File yourFile = createFile(catalog, name + "." + FilenameUtils.getExtension(fileData.getName()));
		Files.write(yourFile.toPath(), fileData.getContents());
	}

	private void saveFileBasedOnDataURI(String catalog, String name, String logoURI) throws IOException
	{
		int dataStartIndex = logoURI.indexOf(",") + 1;
		String data = logoURI.substring(dataStartIndex);
		byte[] decoded = getDecoder().decode(data);
		String mimeType = StringUtils.substringBetween(logoURI, ":", ";");
		File yourFile = createFile(catalog, name + "." + getExtension(mimeType));
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
				.orElse(trustedIdPKey.asString()) + ((locale == null || locale.toString().isBlank()) ? defaultLocale : locale);
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

	private String getExtension(String mimeType)
	{
		return MimeToExtensionTranslator.translateMimeTypeToExtension(mimeType);
	}

	static class MimeToExtensionTranslator
	{
		private static Map<String, String> mimeTypesToExtensions = Map.ofEntries(
			Map.entry("image/bmp", "bmp"),
			Map.entry("image/gif", "gif"),
			Map.entry("image/png", "png"),
			Map.entry("image/jpeg", "jpeg"),
			Map.entry("image/svg+xml", "svg"),
			Map.entry("image/x-icon", "ico"),
			Map.entry("image/vnd.microsoft.icon", "ico")
		);

		private static String translateMimeTypeToExtension(String mimeType)
		{
			return mimeTypesToExtensions.get(mimeType);
		}
	}
}
